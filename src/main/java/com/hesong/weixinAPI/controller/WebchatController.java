package com.hesong.weixinAPI.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;

import redis.clients.jedis.Jedis;

import com.hesong.sugarCRM.HttpClientUtil;
import com.hesong.weixinAPI.context.ContextPreloader;
import com.hesong.weixinAPI.core.MessageRouter;
import com.hesong.weixinAPI.job.CheckSessionAvailableJob;
import com.hesong.weixinAPI.job.CheckWeiboSessionAvailableJob;
import com.hesong.weixinAPI.model.ChatMessage;
import com.hesong.weixinAPI.model.Staff;
import com.hesong.weixinAPI.model.StaffSessionInfo;
import com.hesong.weixinAPI.model.WaitingClient;
import com.hesong.weixinAPI.tools.API;
import com.hesong.weixinAPI.tools.EncryptDecryptData;
import com.hesong.weixinAPI.tools.WeChatHttpsUtil;


@Controller
@RequestMapping("/webchat")
public class WebchatController {
    private static Logger log = Logger.getLogger(WebchatController.class);
    
    private static int DEFFER_TIME = 0;
    
    private final Map<String, DeferredResult<ChatMessage>> chatRequests = new ConcurrentHashMap<String, DeferredResult<ChatMessage>>();
    
    @RequestMapping(method = RequestMethod.GET)
    public String index(
            @CookieValue(value = "WX_STF_UID", defaultValue = "") String staff_uuid) {
        log.info("staff_uuid=" + staff_uuid);
        if (staff_uuid.equals("")) {
            return "webchatError";
        }
        String url = API.SUA_STAFF_WEB_LOGIN_URL + staff_uuid;
        JSONObject staff_info = JSONObject.fromObject(HttpClientUtil.httpGet(url));
        if (staff_info.getBoolean("success")){
            return "chatList";
        }
        
        return "webchatError";
    }
    
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST)
    public String indexPost(@RequestParam("staff_uuid") String staff_uuid,
            HttpServletResponse response) {
        log.info("staff_uuid=" + staff_uuid);
        response.addCookie(new Cookie("WX_STF_UID", staff_uuid));
        return "ok";
    }
    
    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login(){
        return "login";
    }
    
    @RequestMapping(value = "/warning", method = RequestMethod.GET)
    public String warning(){
        return "webchatError";
    }
    
    @RequestMapping(value = "/{staff_uuid}/loginFromSua", method = RequestMethod.GET)
    public String loginFromSua(@PathVariable String staff_uuid,
            HttpServletResponse response) {
        response.addCookie(new Cookie("WX_STF_UID", staff_uuid));
        log.info("loginFromSua: " + staff_uuid);
        return "checkStaffUid";
    }
    
    @ResponseBody
    @RequestMapping(value = "/{staff_uuid}/getStaffInfo", method = RequestMethod.GET)
    public JSONObject getStaffInfo(@PathVariable String staff_uuid){
        String url = API.SUA_STAFF_WEB_LOGIN_URL + staff_uuid;
        try {

            JSONObject staff_info = JSONObject.fromObject(HttpClientUtil
                    .httpGet(url));
            if (staff_info.getBoolean("success")) {
                String tenantUn = staff_info.getJSONObject("person").getJSONObject("tenant").getString(
                        "tenantUn");
                if (MessageRouter.mulClientStaffMap.containsKey(tenantUn)
                        && MessageRouter.mulClientStaffMap.get(tenantUn)
                                .containsKey(staff_uuid)) {
                    staff_info.put("isCheckedIn", true);
                } else {
                    staff_info.put("isCheckedIn", false);
                }
            }
            return staff_info;
        } catch (Exception e) {
            e.printStackTrace();
            return createErrMsg(e.toString());
        }
    }
    
    @SuppressWarnings("unchecked")
    @ResponseBody
    @RequestMapping(value = "/loginRequest", method = RequestMethod.POST)
    public JSONObject login(HttpServletRequest request, HttpServletResponse response){
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> messageMap;
        try {
            messageMap = mapper.readValue(request.getInputStream(), Map.class);
            String account = messageMap.get("account");
            String password = messageMap.get("password");
            password = EncryptDecryptData.encrypt(API.DES_KEY, password);
            Map<String, String> requestMap = new HashMap<String, String>();
            requestMap.put("loginName", account);
            requestMap.put("password", password);
            String ret = HttpClientUtil.httpPost(API.SUA_STAFF_WEB_LOGIN_REQUEST, requestMap);
            JSONObject login = JSONObject.fromObject(ret);
            if (login.getBoolean("success")) {
                response.addCookie(new Cookie("WX_STF_UID", login.getJSONObject("person").getString("id")));
                return login;
            }
            return createErrMsg("Login failed");
        } catch(Exception e) {
            e.printStackTrace();
            log.error("Login failed: " + e.toString());
            return createErrMsg(e.toString());
        }
    }
    
    @ResponseBody
    @RequestMapping(value = "/{staff_uuid}/checkin", method = RequestMethod.GET)
    public JSONObject checkin(@PathVariable String staff_uuid, HttpSession session, HttpServletResponse response) {
        String url = API.SUA_STAFF_WEB_LOGIN_URL + staff_uuid;
//        if(!WeChatHttpsUtil.jedisNotExistThenHSet(API.REDIS_WEIXIN_WEBCHAT_SESSIONID, staff_uuid, session.getId())) {
//            return createErrMsg("请不要重复签入: " + staff_uuid);
//        }
        try {
            JSONObject login_info = JSONObject.fromObject(HttpClientUtil.httpGet(url));
            log.info("Checkin SUA return: " + login_info.toString());
            if (login_info.getBoolean("success")) {
                JSONObject staff_info = login_info.getJSONObject("person");
                JSONObject tenant = staff_info.getJSONObject("tenant");
                String tenantUn = tenant.getString("tenantUn");
                
                Map<String, Staff> staff_map = null;
                
                if (MessageRouter.mulClientStaffMap.containsKey(tenantUn)) {
                    staff_map = MessageRouter.mulClientStaffMap.get(tenantUn);
                    if (staff_map.containsKey(staff_uuid)) {
                        log.info("Staff already checked in, staff_uuid: " + staff_uuid);
                        
                        Staff staff = staff_map.get(staff_uuid);
                        for (StaffSessionInfo s : staff.getSessionChannelList()) {
                            s.setWebStaff(true);
                        }
                        response.addCookie(new Cookie("WX_STF_UID", staff_uuid));
                        return login_info;
                    }
                } else {
                    staff_map = new HashMap<String, Staff>();
                    MessageRouter.mulClientStaffMap.put(tenantUn, staff_map);
                }
                // Create staff
                String staff_working_num = staff_info.getString("number");
                String staff_name = staff_info.getString("name");
                
                JSONArray channel_list = staff_info.getJSONArray("channels");
                
                List<StaffSessionInfo> sessionChannelList = new ArrayList<StaffSessionInfo>();
                String text = String.format("系统提示:您的账户已经通过网页签到成功,您的工号是%s.", staff_working_num);
                
                for (int i = 0; i < channel_list.size(); i++) {
                    JSONObject channel = channel_list.getJSONObject(i);
                    String staff_account = channel.getString("weixinId");
                    String staff_openid = channel.getString("openId");
                    StaffSessionInfo s = new StaffSessionInfo(tenantUn, staff_account, staff_openid, staff_working_num, staff_name, staff_uuid);
                    s.setWebStaff(true);
                    MessageRouter.activeStaffMap.put(staff_openid, s);
                    sessionChannelList.add(s);
                    JSONObject staff_account_id = new JSONObject();
                    staff_account_id.put("wx_account", staff_account);
                    staff_account_id.put("staffid", staff_uuid);
                    staff_account_id.put("tenantUn", tenantUn);
                    MessageRouter.staffIdList.put(staff_openid, staff_account_id);

                    MessageRouter.sendMessage(staff_openid, MessageRouter.getAccessToken(staff_account), text, API.TEXT_MESSAGE);
                }
                
                List<String> skills = new ArrayList<String>();
                if (staff_info.containsKey("skills")) {
                    JSONArray skl = staff_info.getJSONArray("skills");
                    for (int i = 0; i < skl.size(); i++) {
                        skills.add(skl.getJSONObject(i).getString("code"));
                    }
                }
                
                log.info("staffIdList: "+MessageRouter.staffIdList.toString());
                Staff staff = new Staff(staff_uuid, staff_name, tenantUn, staff_working_num, sessionChannelList, skills);
                staff_map.put(staff_uuid, staff);
                log.info("Staff checked in: " + staff.toString());
                response.addCookie(new Cookie("WX_STF_UID", staff_uuid));
                WeChatHttpsUtil.jedisNotExistThenHSet(API.REDIS_WEIXIN_WEBCHAT_SESSIONID, staff_uuid, session.getId());
                return login_info;
            } else {
                log.error("Staff checkin failed: " + login_info.getString("msg"));
                return createErrMsg(login_info.getString("msg"));
            }
        } catch (Exception e) {
            log.error("Staff checkin failed: " + e.toString());
            return createErrMsg("系统出错，请联系管理员！");
        }
    }
    
    @ResponseBody
    @RequestMapping(value = "/{tenantUn}/checkout/{staff_uuid}", method = RequestMethod.GET)
    public JSONObject checkout(@PathVariable String tenantUn, @PathVariable String staff_uuid) {
        if (MessageRouter.mulClientStaffMap.containsKey(tenantUn)) {
            Map<String, Staff> staff_map = MessageRouter.mulClientStaffMap.get(tenantUn);
            if (staff_map.containsKey(staff_uuid)) {
                
                Staff staff = staff_map.get(staff_uuid);
                
                Jedis jedis = ContextPreloader.jedisPool.getResource();
                
                for (StaffSessionInfo s : staff.getSessionChannelList()) {
                    if (s.isBusy()) {
                        s.setBusy(false);
                        s.setWebStaff(false);
                        
                        if (s.getClient_type().equalsIgnoreCase("wx")) {
                         // Remaind client that staff is leaving.
                            String token = jedis.hget(API.REDIS_WEIXIN_ACCESS_TOKEN_KEY, s.getClient_account()); //ContextPreloader.Account_Map.get(s.getClient_account()).getToken();
                            String text = "对不起，客服MM有急事下线了,会话已结束。您可以使用留言功能,客服MM将会在第一时间给您回复[微笑]";
                            MessageRouter.sendMessage(s.getClient_openid(), token, text, "text");

                            if (CheckSessionAvailableJob.sessionMap.containsKey(tenantUn)) {
                                CheckSessionAvailableJob.sessionMap.get(tenantUn).remove(s.getClient_openid());
                            }
                        }
                        
                        if (CheckWeiboSessionAvailableJob.weiboSessionMap.containsKey(tenantUn) && 
                                CheckWeiboSessionAvailableJob.weiboSessionMap.get(tenantUn).containsKey(s.getClient_openid())) {
                            CheckWeiboSessionAvailableJob.weiboSessionMap.get(tenantUn).remove(s.getClient_openid());
                        }
                        
                        s.setEndTime(API.TIME_FORMAT.format(new Date()));
                        MessageRouter.recordSession(s, 0);
                    }
                    String token = jedis.hget(API.REDIS_WEIXIN_ACCESS_TOKEN_KEY, s.getAccount()); // ContextPreloader.Account_Map.get(s.getAccount()).getToken();
                    MessageRouter.sendMessage(s.getOpenid(), token, "系统提示：您已经从网页端成功签出!", API.TEXT_MESSAGE);
                    MessageRouter.activeStaffMap.remove(s.getOpenid());
                    MessageRouter.staffIdList.remove(s.getOpenid());
                }
                staff_map.remove(staff_uuid);
                
                jedis.hdel(API.REDIS_WEIXIN_WEBCHAT_SESSIONID, staff_uuid);
                ContextPreloader.jedisPool.returnResource(jedis);
                
                return WeChatHttpsUtil.getErrorMsg(0, "ok");
            } else {
                return WeChatHttpsUtil.getErrorMsg(1, "客服签出失败，客服ID不存在: " + staff_uuid);
            }
        } else {
            return WeChatHttpsUtil.getErrorMsg(1, "客服签出失败，租户标识不存在: " + tenantUn);
        }
    }
    
    @ResponseBody
    @RequestMapping(value = "/{staff_openid}/takeClient", method = RequestMethod.GET)
    public void takeClient(@PathVariable String staff_openid){
        StaffSessionInfo session = MessageRouter.activeStaffMap.get(staff_openid);
        ChatMessage msg = new ChatMessage();
        msg.setChannelId(staff_openid);
        msg.setMsgtype("sysMessage");
        msg.setSenderName(staff_openid);
        msg.setAction("");
        if (session.isBusy()) {
            String text = "系统提示：您正在和客户通话,无法实施该操作.";
            msg.setContent(text);
            processMessage(msg, session.getStaff_uuid());
            return;
        }
        
        String tenentUn = session.getTenantUn();
        if (!MessageRouter.mulClientStaffMap.containsKey(tenentUn) || !MessageRouter.mulClientStaffMap.get(tenentUn).containsKey(session.getStaff_uuid())) {
            String text = "系统提示：您还没有签到，无法使用此功能!";
            msg.setContent(text);
            processMessage(msg, session.getStaff_uuid());
            return;
        }
        
        if (!MessageRouter.waitingList.containsKey(tenentUn)) {
            String text = "系统提示：请求已被其他客服抢接或没有客户发起人工请求.";
            msg.setContent(text);
            processMessage(msg, session.getStaff_uuid());
            return;
        }
        
        Staff staff = MessageRouter.mulClientStaffMap.get(tenentUn).get(session.getStaff_uuid());
        Map<String, Queue<WaitingClient>> c_map = MessageRouter.waitingList.get(tenentUn);
        WaitingClient client = null;
        for (String skill : staff.getSkills()) {
            if (c_map.containsKey(skill) && !c_map.get(skill).isEmpty()) {
                client = c_map.get(skill).poll();
                break;
            }
        }
        if (null == client) {
            String text = "系统提示：请求已被其他坐席抢接或没有客户发起人工请求.";
            msg.setContent(text);
            processMessage(msg, session.getStaff_uuid());
            return;
        }
        
        Map<String, StaffSessionInfo> client_session = CheckSessionAvailableJob.sessionMap.get(session.getTenantUn());
        if (null == client_session) {
            client_session = new ConcurrentHashMap<String, StaffSessionInfo>();
            CheckSessionAvailableJob.sessionMap.put(tenentUn, client_session);
        }
        
        if (!client_session.containsKey(client.getOpenid())) {
            // Remove client openid from waiting list
            MessageRouter.waitingListIDs.remove(client.getOpenid());

            session.setBusy(true);
            session.setLastReceived(new Date());
            session.setClient_openid(client.getOpenid());
            session.setClient_account(client.getAccount());
            session.setClient_name(client.getName());
            session.setClient_type("wx");
            session.setSession(UUID.randomUUID().toString());
            session.setBeginTime(API.TIME_FORMAT.format(new Date()));
            client_session.put(client.getOpenid(), session);
            
            // To staff
            String text = String.format("系统提示：您已经和客户\"%s\"建立通话.", client.getName());
            String sToken = MessageRouter.getAccessToken(session.getAccount());
            MessageRouter.sendMessage(staff_openid, sToken, text, API.TEXT_MESSAGE);
            // To web
            msg.setContent(text);
            msg.setAction("takeClient");
            processMessage(msg, session.getStaff_uuid());
            // To client
            text = String.format("系统提示：会话已建立,客服%s将为您服务[微笑]", session.getStaffid());
            MessageRouter.sendMessage(client.getOpenid(), MessageRouter.getAccessToken(client.getAccount()), text, API.TEXT_MESSAGE);
            
        } else {
            String text = "系统提示：系统错误，请联系管理员.";
            msg.setContent(text);
            processMessage(msg, session.getStaff_uuid());
        }
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/{staff_uuid}/sendWebMessage", method = RequestMethod.POST)
    public @ResponseBody
    JSONObject sendWebMessage(@PathVariable String staff_uuid,
            HttpServletRequest request) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> messageMap;
        try {
            messageMap = mapper.readValue(request.getInputStream(), Map.class);
            String content = messageMap.get("content");

            ChatMessage msg = new ChatMessage();
            String msgtype = messageMap.get("msgtype");
            msg.setContent(content);
            // msg.setDate(sdf.format(new Date()));
            msg.setSenderName(messageMap.get("senderName"));
            msg.setChannelId(messageMap.get("channelId"));
            msg.setMsgtype(msgtype);
            msg.setAction(messageMap.get("action"));
            // 发布消息给所有用户
            processMessage(msg, staff_uuid);
            log.info("Msg: " + msg.toString());
            log.info("staff_uuid: " + staff_uuid);
            return WeChatHttpsUtil.getErrorMsg(0, "OK");

        } catch (Exception e) {
            log.error("Json mapper exception: " + e.toString());
            return WeChatHttpsUtil.getErrorMsg(7999, e.toString());
        }
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/{staff_uuid}/sendWeixinMsg", method = RequestMethod.POST)
    public @ResponseBody
    String sendWeixinMsg(@PathVariable String staff_uuid,
            HttpServletRequest request) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> messageMap;
        try {
            messageMap = mapper.readValue(request.getInputStream(), Map.class);
            String account = messageMap.get("account");
            String openid = messageMap.get("channelId");
            String content = messageMap.get("content");

            String msgtype = messageMap.get("msgtype");
            
//            ChatMessage msg = new ChatMessage();
//            
//            // send to web
//            msg.setSenderName(messageMap.get("senderName"));
//            msg.setChannelId(openid);
//            msg.setMsgtype(msgtype);
//            msg.setContent(content);
//            processMessage(msg, staff_uuid);
            
            StaffSessionInfo s = MessageRouter.activeStaffMap.get(openid);
            if (null != s && s.isBusy()) {
                String text = String.format("客服%s：%s", s.getStaffid(), content);
                // Send to staff
                String sToken = MessageRouter.getAccessToken(account);
                MessageRouter.sendMessage(openid, sToken, text, msgtype);

                // Send to client
                String cToken = MessageRouter.getAccessToken(s.getClient_account());
                MessageRouter.sendMessage(s.getClient_openid(), cToken, text, msgtype);
                
                s.setLastReceived(new Date());
            }
            
            return "Success";
            

        } catch (IOException e) {
            log.error("Json mapper exception: " + e.toString());
            return "Failed";
        }
    }
    
    @ResponseBody
    @RequestMapping(value = "/{staff_uuid}/getMessages", method = RequestMethod.GET)
    public DeferredResult<ChatMessage> getMessages(@PathVariable final String staff_uuid, HttpServletRequest request) {
        // 创建DeferredResult<Message>
        DeferredResult<ChatMessage> dr = new DeferredResult<ChatMessage>(DEFFER_TIME);
        // 若用户不存在则直接返回，否则将其放入用户请求列表中然后返回
        if (null == staff_uuid) {
            log.warn("staff_uuid is null");
            return dr;
        } else {
            // 当DeferredResult对客户端响应后将其从列表中移除
            dr.onCompletion(new Runnable() {
                @Override
                public void run() {
                    chatRequests.remove(staff_uuid);
                }
            });
            chatRequests.put(staff_uuid, dr);
            return dr;
        }
    }
    
    @RequestMapping(value = "/{staff_uuid}/upload", method = RequestMethod.POST)
    @ResponseBody
    public String upload(@PathVariable String staff_uuid, @RequestParam("Filedata") MultipartFile mulpartFile,
            @RequestParam("roomId") String roomId, HttpServletRequest request) {
        if (roomId == null) {
            log.error("RoomId is not exist.");
            return "Failed";
        }
        
        StaffSessionInfo s = MessageRouter.activeStaffMap.get(roomId);
        if (null != s && s.isBusy() && s.isWebStaff()) {
            String token = MessageRouter.getAccessToken(s.getClient_account());
            String post_url = API.UPLOAD_IMAGE_REQUEST_URL.replace("ACCESS_TOKEN", token) + "image";
            
            JSONObject ret;
            try {
                ret = WeChatHttpsUtil.httpPostFile(post_url, mulpartFile.getInputStream(), UUID.randomUUID().toString() + ".jpg");
                log.info("Upload return: "+ret.toString());
                if (ret.containsKey("media_id")) {
                    String media_id = ret.getString("media_id");
                    MessageRouter.sendMessage(s.getClient_openid(), token, media_id, API.IMAGE_MESSAGE);
                    return API.WEIXIN_MEDIA_URL.replace("ACCESS_TOKEN", token) + media_id;
                } 
            } catch (IOException e) {
                log.error("Uploader image failed: " + e.toString());
                e.printStackTrace();
            }
            
        } else {
            return "Failed";
        }
         
        return "Failed";
        
    }
    
    private void processMessage(ChatMessage msg, String staff_uuid) {
        log.info("processMessage: " + msg.toString());
        DeferredResult<ChatMessage> tmp = chatRequests.get(staff_uuid);
        if (tmp == null) {
            log.warn("DeferredResult is null.");
            return;
        } else {
            tmp.setResult(msg);
        }
    }
    
    private static JSONObject createErrMsg(String msg) {
        JSONObject jo = new JSONObject();
        jo.put("success", false);
        jo.put("errmsg", msg);
        return jo;
    }
}
