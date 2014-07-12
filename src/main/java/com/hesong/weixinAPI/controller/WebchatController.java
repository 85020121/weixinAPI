package com.hesong.weixinAPI.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.async.DeferredResult;

import com.hesong.sugarCRM.HttpClientUtil;
import com.hesong.weixinAPI.core.MessageRouter;
import com.hesong.weixinAPI.model.ChatMessage;
import com.hesong.weixinAPI.model.Staff;
import com.hesong.weixinAPI.model.StaffSessionInfo;
import com.hesong.weixinAPI.tools.API;
import com.hesong.weixinAPI.tools.WeChatHttpsUtil;


@Controller
@RequestMapping("/webchat")
public class WebchatController {
    private static Logger log = Logger.getLogger(WebchatController.class);
    
    private static int DEFFER_TIME = 15000;
    private static String CHECKIN_URL = "http://www.clouduc.cn/sua/rest/n/tenant/kfCheckInInfo?idtype=uuid&id=";
    
    private final Map<String, DeferredResult<ChatMessage>> chatRequests = new ConcurrentHashMap<String, DeferredResult<ChatMessage>>();
    private final Map<String, JSONArray> channelList = new ConcurrentHashMap<String, JSONArray>();
    
    @RequestMapping(value = "/{staff_uuid}/checkin", method = RequestMethod.GET)
    public String checkin(@PathVariable String staff_uuid, HttpSession session, HttpServletResponse response) {
        String url = CHECKIN_URL + staff_uuid;
        response.addCookie(new Cookie("WX_STF_UID", staff_uuid));
        session.setAttribute("WX_STF_UID", staff_uuid);
        try {
            JSONObject staff_info = JSONObject.fromObject(HttpClientUtil.httpGet(url));
            log.info("Checkin SUA return: " + staff_info.toString());
            if (staff_info.getBoolean("success")) {
                if (staff_info.getInt("tenantstatus") != 1) {
                    log.warn("Staff no auth");
                    // String text = "欢迎使用和声云客服系统，您申请成为【商家】客服，正在审核中，请耐心等待...";
                    // TODO send message
                    return "error";
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
                        
                        //String text = "系统消息:您已经签入了,无需再次签入.";
                        // TODO send message
                        return "chatRoom";
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
                String text = String.format("系统提示:签到成功,你的工号是%s.", staff_working_num);
                
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
                return "chatRoom";
            } else {
                log.error("Staff checkin failed: " + staff_info.getString("msg"));
                // TODO send message
            }
        } catch (Exception e) {
            log.error("Staff checkin failed: " + e.toString());
            // TODO send message
        }
        return null;
    }
    
    @ResponseBody
    @RequestMapping(value = "/{tenantUn}/closeTab/{staff_uuid}", method = RequestMethod.GET)
    public String closeTab(@PathVariable String tenantUn, @PathVariable String staff_uuid) {
        if (MessageRouter.mulClientStaffMap.containsKey(tenantUn)) {
            Map<String, Staff> staff_map = MessageRouter.mulClientStaffMap.get(tenantUn);
            if (staff_map.containsKey(staff_uuid)) {
                log.info("Web tab closed: " + staff_uuid);
                
                Staff staff = staff_map.get(staff_uuid);
                for (StaffSessionInfo s : staff.getSessionChannelList()) {
                    s.setWebStaff(false);
                }
                
                //String text = "系统消息:您已经签入了,无需再次签入.";
                // TODO send message
                return "OK";
            }
        }
        return "False";
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
            
            // TODO send to weixin
            String sToken = MessageRouter.getAccessToken(account);
            // Send to staff
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
        log.info("staff_uuid: " + staff_uuid);
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
        DeferredResult<ChatMessage> tmp = chatRequests.get(staff_uuid);
        if (tmp == null) {
            return;
        } else {
            tmp.setResult(msg);
        }
    }
}
