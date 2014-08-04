package com.hesong.weixinAPI.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import redis.clients.jedis.Jedis;

import com.hesong.weixinAPI.context.ContextPreloader;
import com.hesong.weixinAPI.core.MessageExecutor;
import com.hesong.weixinAPI.core.MessageRouter;
import com.hesong.weixinAPI.job.CheckSessionAvailableJob;
import com.hesong.weixinAPI.job.CheckWaitingListJob;
import com.hesong.weixinAPI.model.AccessToken;
import com.hesong.weixinAPI.model.WaitingClient;
import com.hesong.weixinAPI.tools.API;
import com.hesong.weixinAPI.tools.WeChatHttpsUtil;

@Controller
@RequestMapping("/utils")
public class UtilsController {
    
    private static Logger log = Logger.getLogger(UtilsController.class);
    
    @ResponseBody
    @RequestMapping(value = "/{scene_id}/getQRcodeList", method = RequestMethod.GET)
    public String getQRcodeList(@PathVariable int scene_id) {
        JSONArray qrCodeList = new JSONArray();
        log.info("Create QRCode with scene_id: " + scene_id);
        for (String account : ContextPreloader.staffAccountList) {
            String token = MessageRouter.getAccessToken(account);
            String qrCode = WeChatHttpsUtil.getQRCode(token, scene_id);
            if(null == qrCode) continue;
            qrCodeList.add(qrCode);
        }
        return qrCodeList.toString();
    }
    
    @ResponseBody
    @RequestMapping(value = "/sendMessage", method = RequestMethod.POST)
    public String sendMessage(HttpServletRequest request) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JSONObject json = (JSONObject) JSONSerializer.toJSON(mapper
                    .readValue(request.getInputStream(), Map.class));
            String account = json.getString("account");
            json.remove("account");
            String token = MessageRouter.getAccessToken(account);
            String type = json.getString("msgtype");
            String content = json.getString("content");
            
            JSONArray openid_list = json.getJSONArray("touser");
            for (Object openid : openid_list) {
                MessageExecutor.sendMessage((String)openid, token, content, type);
            }
            return WeChatHttpsUtil.getErrorMsg(0, "Message sended.").toString();
        } catch (Exception e) {
            e.printStackTrace();
            return WeChatHttpsUtil.getErrorMsg(1, "Send message failed: " + e.toString()).toString();
        }
    }
    
    @ResponseBody
    @RequestMapping(value = "/checkClientAccount", method = RequestMethod.POST)
    public String checkClientAccount(HttpServletRequest request) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JSONObject json = (JSONObject) JSONSerializer.toJSON(mapper
                    .readValue(request.getInputStream(), Map.class));
//            String account = json.getString("account");
            String appid = json.getString("appId");
            String appsecret = json.getString("appSecret");
            
            String requestUrl = API.ACCESS_TOKEN_URL.replace("APPID", appid).replace(
                    "APPSECRET", appsecret);
            JSONObject jo = WeChatHttpsUtil.httpsRequest(requestUrl, "GET", null);
            if (!jo.containsKey("access_token")) {
                return jo.toString();
            }
            return WeChatHttpsUtil.getErrorMsg(0, "OK").toString();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Add new client account failed: " + e.toString());
            return WeChatHttpsUtil.getErrorMsg(1, "Client account info is not available, check it again.").toString();
        }
    }
    
    @ResponseBody
    @RequestMapping(value = "/addNewClientAccount", method = RequestMethod.POST)
    public String addNewClient(HttpServletRequest request) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JSONObject json = (JSONObject) JSONSerializer.toJSON(mapper
                    .readValue(request.getInputStream(), Map.class));
            String tenantUn = json.getString("tenantUn");
            String account = json.getString("account");
            String appid = json.getString("appid");
            String appsecret = json.getString("appsecret");
            AccessToken ac = new AccessToken(tenantUn, account, appid, appsecret);
            Jedis jedis = ContextPreloader.jedisPool.getResource();
            WeChatHttpsUtil.setAccessTokenToRedis(jedis, account, appid, appsecret, tenantUn, API.REDIS_CLIENT_ACCOUNT_INFO_KEY);
            ContextPreloader.jedisPool.returnResource(jedis);
            log.info("New client added into Account_Map: " + ac.toString());
            return WeChatHttpsUtil.getErrorMsg(0, "Client added.").toString();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Add new client account failed: " + e.toString());
            return WeChatHttpsUtil.getErrorMsg(1, "Add client failed: " + e.toString()).toString();
        }
    }
    
    @ResponseBody
    @RequestMapping(value = "/{tenantUn}/setClientMenuIVR", method = RequestMethod.POST)
    public String setClientMenuIVR(@PathVariable String tenantUn, HttpServletRequest request) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JSONArray json = JSONArray.fromObject(mapper
                    .readValue(request.getInputStream(),JSONArray.class));
            log.info("IVR menu: " + json.toString());
            List<String> keyWords = new ArrayList<String>();
            JSONArray events = new JSONArray();
            String keywords_tag = API.REDIS_CLIENT_TEXT_IVR + tenantUn;
            String events_tag = API.REDIS_CLIENT_EVENT_IVR + tenantUn;
            
            Jedis jedis = ContextPreloader.jedisPool.getResource();
            
            for (int i = 0; i < json.size(); i++) {
                JSONObject jo = json.getJSONObject(i);
                String msgtype = jo.getString("msgtype");
                String keyword = jo.getString("eventkey");
                String replytype = jo.getString("replytype");
                JSONObject replycontent = jo.getJSONObject("replycontent");
                replycontent.put("msgtype", replytype);
                if (msgtype.equalsIgnoreCase(API.TEXT_MESSAGE)) {
                    keyWords.add(keyword);
                    jedis.hset(keywords_tag, keyword, replycontent.toString());
                } else if (msgtype.equals(API.EVENT_MESSAGE)) {
                    String event = jo.getString("event");
                    if (event.equalsIgnoreCase(API.CLICK_EVENT)) {
                        events.add(keyword);
                        jedis.hset(events_tag, keyword, replycontent.toString());
                    } else {
                        events.add(event);
                        jedis.hset(events_tag, event, replycontent.toString());
                    }
                }
            }
            
            if (!keyWords.isEmpty()) {
                String regex = makeRegex(keyWords);
                jedis.hset(API.REDIS_CLIENT_KEYWORDS_REGEX, tenantUn, regex);
                log.info("Keywords regex: " + regex);
            }
            
            if (!events.isEmpty()) {
                jedis.hset(API.REDIS_CLIENT_EVENT_LIST, tenantUn, events.toString());
                log.info("Events key: " + events.toString());
            }
            
            ContextPreloader.jedisPool.returnResource(jedis);
            
            return WeChatHttpsUtil.getErrorMsg(0, "ok").toString();
        } catch (Exception e) {
            log.error("Json error.");
            e.printStackTrace();
            return WeChatHttpsUtil.getErrorMsg(1, e.toString()).toString();
        }
    }
    
    @ResponseBody
    @RequestMapping(value = "/{tenantUn}/waitingListCount", method = RequestMethod.GET)
    public int waitingListCount(@PathVariable String tenantUn) {
        if (MessageRouter.waitingList.containsKey(tenantUn)) {
            int count = 0;
            Map<String, Queue<WaitingClient>> map = MessageRouter.waitingList.get(tenantUn);
            for (String key : map.keySet()) {
                count = count + map.get(key).size();
            }
            return count;
        } else {
            return 0;
        }
    }
    
    @ResponseBody
    @RequestMapping(value = "/{tenantUn}/isStaffCheckedIn/{staff_uuid}", method = RequestMethod.GET)
    public boolean isStaffCheckedIn(@PathVariable String tenantUn, @PathVariable String staff_uuid) {
        return (MessageRouter.mulClientStaffMap.containsKey(tenantUn) 
                && MessageRouter.mulClientStaffMap.get(tenantUn).containsKey(staff_uuid)); 
    }
    
    @ResponseBody
    @RequestMapping(value = "/{tenantUn}/setSessionDuration/{duration}", method = RequestMethod.GET)
    public String setSessionDuration(@PathVariable String tenantUn, @PathVariable String duration) {
        try {
            CheckSessionAvailableJob.session_available_duration_map.put(tenantUn, Long.parseLong(duration));
            return WeChatHttpsUtil.getErrorMsg(0, "ok").toString();
        } catch (Exception e) {
            return WeChatHttpsUtil.getErrorMsg(1, e.toString()).toString();
        }
    }
    
    @ResponseBody
    @RequestMapping(value = "/{tenantUn}/setWaitingDuration/{duration}", method = RequestMethod.GET)
    public String setWaitingDuration(@PathVariable String tenantUn, @PathVariable String duration) {
        try {
            CheckWaitingListJob.waiting_duration_map.put(tenantUn, Long.parseLong(duration));
            return WeChatHttpsUtil.getErrorMsg(0, "ok").toString();
        } catch (Exception e) {
            return WeChatHttpsUtil.getErrorMsg(1, e.toString()).toString();
        }
    }
    
    @ResponseBody
    @RequestMapping(value = "/{tenantUn}/setClientSkills", method = RequestMethod.POST)
    public String setClientSkills(@PathVariable String tenantUn, HttpServletRequest request) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JSONArray json = JSONArray.fromObject(mapper
                    .readValue(request.getInputStream(),JSONArray.class));
            log.info("setClientSkills: " + json.toString());
            return WeChatHttpsUtil.getErrorMsg(0, "ok").toString();
        } catch (Exception e) {
            log.error("Json error.");
            e.printStackTrace();
            return WeChatHttpsUtil.getErrorMsg(1, e.toString()).toString();
        }
    }
    
    private String makeRegex(List<String> list) {
        String regex = null;
        for (int i = 0; i < list.size(); i++) {
            if (i == 0) {
                regex = ".*(" + list.get(i);
            }
            if (i > 0) {
                regex = regex + "|" + list.get(i);
            }
            if (i == list.size() - 1) {
                regex = regex + ").*";
                break;
            }
        }
        return regex;
    }
    
}