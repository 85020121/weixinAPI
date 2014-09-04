package com.hesong.weixinAPI.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import com.hesong.weixinAPI.model.AccessToken;
import com.hesong.weixinAPI.model.StaffSessionInfo;
import com.hesong.weixinAPI.model.WaitingClient;
import com.hesong.weixinAPI.tools.API;
import com.hesong.weixinAPI.tools.RedisOperations;
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
        log.info("IP: " + request.getHeader("x-real-ip"));
        ObjectMapper mapper = new ObjectMapper();
        try {
            JSONObject json = (JSONObject) JSONSerializer.toJSON(mapper
                    .readValue(request.getInputStream(), Map.class));
            String account = json.getString("account");
            json.remove("account");
            String token = MessageRouter.getAccessToken(account);
            String type = json.getString("msgtype");
            String content = json.getString("content");
            
            if (null == token) {
                return WeChatHttpsUtil.getErrorMsg(1, "Account does not exist: " + account).toString();
            }
            
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
    @RequestMapping(value = "/sendExpressMessage", method = RequestMethod.POST)
    public String sendExpressMessage(HttpServletRequest request) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JSONObject json = (JSONObject) JSONSerializer.toJSON(mapper
                    .readValue(request.getInputStream(), Map.class));
            String openid = json.getString("openid");
            if (MessageRouter.activeStaffMap.containsKey(openid)) {
                StaffSessionInfo session = MessageRouter.activeStaffMap.get(openid);
                if (null != session && session.isBusy()) {
                    String text = json.getString("text");
                    String content = String.format("客服%s：%s", session.getStaffid(), text);
                    
                    String sToken = MessageRouter.getAccessToken(session.getAccount());
                    MessageRouter.sendMessage(openid, sToken, content, API.TEXT_MESSAGE);
                    
                    String cToken = MessageRouter.getAccessToken(session.getClient_account());
                    MessageRouter.sendMessage(session.getClient_openid(), cToken, content, API.TEXT_MESSAGE);
                    
                    MessageRouter.recordMessage(session, content, API.TEXT_MESSAGE, "wx", false);
                    
                    if (session.isWebStaff()) {
                        MessageRouter.sendWebMessage("text", text, session.getOpenid(), session.getStaff_uuid(), session.getStaff_uuid(), "", new JSONObject());
                    }
                    
                    return WeChatHttpsUtil.getErrorMsg(0, "ok").toString();
                } else {
                    return WeChatHttpsUtil.getErrorMsg(10007, "会话不存在或已结束。").toString();
                }
            } else {
                return WeChatHttpsUtil.getErrorMsg(10007, "会话不存在或已结束。").toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return WeChatHttpsUtil.getErrorMsg(10002, "Send message failed: " + e.toString()).toString();
        }
    }
    
    @ResponseBody
    @RequestMapping(value = "/checkClientAccount", method = RequestMethod.POST)
    public String checkClientAccount(HttpServletRequest request) {
        log.info("IP: " + request.getHeader("x-real-ip"));
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
    public int waitingListCount(@PathVariable String tenantUn, HttpServletRequest request) {
        //log.info("IP: " + request.getHeader("x-real-ip"));
        return RedisOperations.getWaitingListCount(tenantUn);
        
//        if (MessageRouter.waitingList.containsKey(tenantUn)) {
//            int count = 0;
//            Map<String, List<WaitingClient>> map = MessageRouter.waitingList.get(tenantUn);
//            for (String key : map.keySet()) {
//                count = count + map.get(key).size();
//            }
//            return count;
//        } else {
//            return 0;
//        }
    }
    
    @ResponseBody
    @RequestMapping(value = "/{account}/getClientInfo/{openid}", method = RequestMethod.GET)
    public JSONObject getClientInfo(@PathVariable String account, @PathVariable String openid) {
        return getWeixinClientInfo(account, openid);
    }
    
    
    @ResponseBody
    @RequestMapping(value = "/{tenantUn}/isStaffCheckedIn/{staff_uuid}", method = RequestMethod.GET)
    public boolean isStaffCheckedIn(@PathVariable String tenantUn, @PathVariable String staff_uuid) {
        return (MessageRouter.mulClientStaffMap.containsKey(tenantUn) 
                && MessageRouter.mulClientStaffMap.get(tenantUn).containsKey(staff_uuid)); 
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
    
    private JSONObject getWeixinClientInfo(String account, String openid) {
        String token = MessageRouter.getAccessToken(account);
        String query = API.USER_INFO_URL.replace("ACCESS_TOKEN", token).replace("OPENID", openid);
        JSONObject user_info = WeChatHttpsUtil.httpsRequest(query, "GET", null);
        return user_info;
    }
}