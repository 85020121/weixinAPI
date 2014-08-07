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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.async.DeferredResult;

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
import com.hesong.weixinAPI.tools.WeChatHttpsUtil;


@Controller
@RequestMapping("/webchat")
public class WebchatController {
    private static Logger log = Logger.getLogger(WebchatController.class);
    
    private static int DEFFER_TIME = 15000;
    
    private final Map<String, DeferredResult<ChatMessage>> chatRequests = new ConcurrentHashMap<String, DeferredResult<ChatMessage>>();
    public static Map<String, JSONArray> channelList = new ConcurrentHashMap<String, JSONArray>();

    
    @RequestMapping(value = "/{staff_uuid}/index", method = RequestMethod.GET)
    public String index(@PathVariable String staff_uuid, HttpServletResponse response){
        response.addCookie(new Cookie("WX_STF_UID", staff_uuid));
        return "webChatIndex";
    }
    
    @RequestMapping(value = "/{staff_uuid}/chatlist", method = RequestMethod.GET)
    public String chatlist(@PathVariable String staff_uuid, HttpServletResponse response){
        response.addCookie(new Cookie("WX_STF_UID", staff_uuid));
        return "chatList";
    }
    
    @ResponseBody
    @RequestMapping(value = "/{tenantUn}/enterChatRoom/{staff_uuid}", method = RequestMethod.GET)
    public JSONArray enterChatRoom(@PathVariable String tenantUn, @PathVariable String staff_uuid,
            @CookieValue(value="JSESSIONID", defaultValue="") String jsessionid, 
            HttpServletResponse response){
        log.info("jsessionid: " + jsessionid);
        String sessionid = WeChatHttpsUtil.jedisHGet(API.REDIS_WEIXIN_WEBCHAT_SESSIONID, staff_uuid);
        log.info("redis: "+ sessionid);
        if (null == sessionid) {
            log.warn("Sessionid is null");
        }
        if (!sessionid.equals(jsessionid)) {
            log.warn("Not indentical web client.");
        }
        
        if (MessageRouter.mulClientStaffMap.containsKey(tenantUn)
                && MessageRouter.mulClientStaffMap.get(tenantUn).containsKey(staff_uuid)) {
            Staff staff = MessageRouter.mulClientStaffMap.get(tenantUn).get(staff_uuid);
            for (StaffSessionInfo s : staff.getSessionChannelList()) {
                s.setWebStaff(true);
            }
        }
        response.addCookie(new Cookie("WX_STF_UID", staff_uuid));
        return channelList.get(staff_uuid);
    }
    
    @ResponseBody
    @RequestMapping(value = "/{staff_uuid}/checkin", method = RequestMethod.GET)
    public String checkin(@PathVariable String staff_uuid, HttpSession session, HttpServletResponse response) {
        String url = API.CHECKIN_URL + staff_uuid;
        log.info("session: " + session.getId());
        if(!WeChatHttpsUtil.jedisNotExistThenHSet(API.REDIS_WEIXIN_WEBCHAT_SESSIONID, staff_uuid, session.getId())) {
            return WeChatHttpsUtil.getErrorMsg(10011, "请不要重复签入: " + staff_uuid).toString();
        }
        try {
            JSONObject staff_info = JSONObject.fromObject(HttpClientUtil.httpGet(url));
            log.info("Checkin SUA return: " + staff_info.toString());
            if (staff_info.getBoolean("success")) {
                if (staff_info.getInt("tenantstatus") != 1) {
                    log.warn("Staff no auth");
                    return WeChatHttpsUtil.getErrorMsg(1, "客服还未通过审核。").toString();
                }
                
                String wx_account = staff_info.getString("wx_account");
                String tenantUn = staff_info.getString("tenantUn");
                
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
                        session.setAttribute("WX_STF_UID", staff_uuid);
                        return WeChatHttpsUtil.getErrorMsg(0, "ok").toString();
                    }
                } else {
                    staff_map = new HashMap<String, Staff>();
                    MessageRouter.mulClientStaffMap.put(tenantUn, staff_map);
                }
                // Create staff
                String staff_working_num = staff_info.getString("staff_number");
                String staff_name = staff_info.getString("staff_name");
                
                JSONArray channel_list = staff_info.getJSONArray("channels");
                channelList.put(staff_uuid, channel_list);
                
                List<StaffSessionInfo> sessionChannelList = new ArrayList<StaffSessionInfo>();
                String text = String.format("系统提示:您的账户已经通过网页签到成功,您的工号是%s.", staff_working_num);
                
                for (int i = 0; i < channel_list.size(); i++) {
                    JSONObject channel = channel_list.getJSONObject(i);
                    String staff_account = channel.getString("account");
                    String staff_openid = channel.getString("openid");
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
                Staff staff = new Staff(staff_uuid, staff_name, tenantUn, wx_account, staff_working_num, sessionChannelList, skills);
                staff_map.put(staff_uuid, staff);
                log.info("Staff checked in: " + staff.toString());
                response.addCookie(new Cookie("WX_STF_UID", staff_uuid));
                session.setAttribute("WX_STF_UID", staff_uuid);
                WeChatHttpsUtil.jedisNotExistThenHSet(API.REDIS_WEIXIN_WEBCHAT_SESSIONID, staff_uuid, session.getId());
                return WeChatHttpsUtil.getErrorMsg(0, "ok").toString();
            } else {
                log.error("Staff checkin failed: " + staff_info.getString("msg"));
                return WeChatHttpsUtil.getErrorMsg(1, staff_info.getString("msg")).toString();
            }
        } catch (Exception e) {
            log.error("Staff checkin failed: " + e.toString());
            return WeChatHttpsUtil.getErrorMsg(1, e.toString()).toString();
        }
    }
    
    @ResponseBody
    @RequestMapping(value = "/{tenantUn}/checkout/{staff_uuid}", method = RequestMethod.GET)
    public String checkout(@PathVariable String tenantUn, @PathVariable String staff_uuid) {
        if (MessageRouter.mulClientStaffMap.containsKey(tenantUn)) {
            Map<String, Staff> staff_map = MessageRouter.mulClientStaffMap.get(tenantUn);
            if (staff_map.containsKey(staff_uuid)) {
                log.info("Web tab closed: " + staff_uuid);
                
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
                    MessageRouter.sendMessage(s.getOpenid(), token, "系统消息:您已成功签出!", API.TEXT_MESSAGE);
                    MessageRouter.activeStaffMap.remove(s.getOpenid());
                    MessageRouter.staffIdList.remove(s.getOpenid());
                }
                String msg = channelList.get(staff_uuid).toString();
                channelList.remove(staff_uuid);
                staff_map.remove(staff_uuid);
                
                jedis.hdel(API.REDIS_WEIXIN_WEBCHAT_SESSIONID, staff_uuid);
                ContextPreloader.jedisPool.returnResource(jedis);
                
                return WeChatHttpsUtil.getErrorMsg(0, msg).toString();
            } else {
                return WeChatHttpsUtil.getErrorMsg(1, "客服签出失败，客服ID不存在: " + staff_uuid).toString();
            }
        } else {
            return WeChatHttpsUtil.getErrorMsg(1, "客服签出失败，租户标识不存在: " + tenantUn).toString();
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
        if (session.isBusy()) {
            String text = "系统消息：您正在和客户通话,无法实施该操作.";
            msg.setContent(text);
            processMessage(msg, session.getStaff_uuid());
            return;
        }
        
        String tenentUn = session.getTenantUn();
        if (!MessageRouter.mulClientStaffMap.containsKey(tenentUn) || !MessageRouter.mulClientStaffMap.get(tenentUn).containsKey(session.getStaff_uuid())) {
            String text = "系统消息：您还没有签到，无法使用此功能!";
            msg.setContent(text);
            processMessage(msg, session.getStaff_uuid());
            return;
        }
        
        if (!MessageRouter.waitingList.containsKey(tenentUn)) {
            String text = "系统消息：请求已被其他客服抢接或没有客户发起人工请求.";
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
            String text = "系统消息：请求已被其他坐席抢接或没有客户发起人工请求.";
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
            String text = String.format("系统消息：您已经和客户\"%s\"建立通话.", client.getName());
            String sToken = MessageRouter.getAccessToken(session.getAccount());
            MessageRouter.sendMessage(staff_openid, sToken, text, API.TEXT_MESSAGE);
            // To web
            msg.setContent(text);
            processMessage(msg, session.getStaff_uuid());
            // To client
            text = String.format("系统消息：会话已建立,客服%s将为您服务[微笑]", session.getStaffid());
            MessageRouter.sendMessage(client.getOpenid(), MessageRouter.getAccessToken(client.getAccount()), text, API.TEXT_MESSAGE);
            
        } else {
            String text = "系统消息：系统错误，请联系管理员.";
            msg.setContent(text);
            processMessage(msg, session.getStaff_uuid());
        }
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/{staff_uuid}/sendWebMessage", method = RequestMethod.POST)
    public @ResponseBody
    JSONObject sendWebMessage(@PathVariable String staff_uuid,
            HttpServletRequest request, HttpSession session) {
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
            HttpServletRequest request, HttpSession session) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> messageMap;
        try {
            messageMap = mapper.readValue(request.getInputStream(), Map.class);
            String account = messageMap.get("account");
            String openid = messageMap.get("channelId");
            String content = messageMap.get("content");

            ChatMessage msg = new ChatMessage();
            String msgtype = messageMap.get("msgtype");
            msg.setContent(content);
            // msg.setDate(sdf.format(new Date()));
            msg.setSenderName(messageMap.get("senderName"));
            msg.setChannelId(openid);
            msg.setMsgtype(msgtype);
            // 发布消息给所有用户
            processMessage(msg, staff_uuid);
            
            // Send to staff
            String sToken = MessageRouter.getAccessToken(account);
            MessageRouter.sendMessage(openid, sToken, content, msgtype);
            
            StaffSessionInfo s = MessageRouter.activeStaffMap.get(openid);
            if (null != s && s.isBusy()) {
                String cToken = MessageRouter.getAccessToken(s.getClient_account());
                // Send to client
                MessageRouter.sendMessage(s.getClient_openid(), cToken, content, msgtype);
            }
            
            return "Success";
            

        } catch (IOException e) {
            log.error("Json mapper exception: " + e.toString());
            return "Failed";
        }
    }
    
    @ResponseBody
    @RequestMapping(value = "/{staff_uuid}/channelList", method = RequestMethod.GET)
    public JSONArray channelList(@PathVariable String staff_uuid) {
        return channelList.get(staff_uuid);
    }

    @ResponseBody
    @RequestMapping(value = "/getMessages", method = RequestMethod.GET)
    public DeferredResult<ChatMessage> getMessages(HttpSession session) {
        // 创建DeferredResult<Message>
        DeferredResult<ChatMessage> dr = new DeferredResult<ChatMessage>(DEFFER_TIME);
        final String staff_uuid = (String) session.getAttribute("WX_STF_UID");
        // 若用户不存在则直接返回，否则将其放入用户请求列表中然后返回
        if (null == staff_uuid) {
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
}
