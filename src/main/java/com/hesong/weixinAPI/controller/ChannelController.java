package com.hesong.weixinAPI.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hesong.weixinAPI.context.ContextPreloader;
import com.hesong.weixinAPI.core.MessageExecutor;
import com.hesong.weixinAPI.core.MessageRouter;
import com.hesong.weixinAPI.job.CheckLeavingMessageJob;
import com.hesong.weixinAPI.job.CheckWeiboSessionAvailableJob;
import com.hesong.weixinAPI.model.LeavingMessageClient;
import com.hesong.weixinAPI.model.Staff;
import com.hesong.weixinAPI.model.StaffSessionInfo;
import com.hesong.weixinAPI.tools.API;
import com.hesong.weixinAPI.tools.WeChatHttpsUtil;


@Controller
@RequestMapping("/channel")
public class ChannelController {
    
    private static Logger log = Logger.getLogger(ChannelController.class);
    
    @ResponseBody
    @RequestMapping(value = "/wb/sx", method = RequestMethod.POST)
    public String weiboSX(HttpServletRequest request) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JSONObject json = (JSONObject) JSONSerializer.toJSON(mapper
                    .readValue(request.getInputStream(), Map.class));
            String tenantUn = json.getString("tenantUn");
            String user_id = json.getString("user_id");
            String user_name = json.getString("user_name");
            String content = json.getString("content");
            String msgtype = json.getString("msgtype");
            String user_image_url = json.getString("user_image_url");
            
            if (null == tenantUn || !MessageRouter.mulClientStaffMap.containsKey(tenantUn) || MessageRouter.mulClientStaffMap.get(tenantUn).isEmpty()) {
                LeavingMessageClient c = getLeavingMessageClient(tenantUn, user_id, user_name, user_image_url);
                c.setSource("wb");
                recordLeaveMessage(c, tenantUn, content, msgtype);
                return WeChatHttpsUtil.getErrorMsg(0, "Message recorded.").toString();
            } else {
                if (!CheckWeiboSessionAvailableJob.weiboSessionMap.containsKey(tenantUn)) {
                    CheckWeiboSessionAvailableJob.weiboSessionMap.put(tenantUn, new HashMap<String, StaffSessionInfo>());
                }
                
                Map<String, StaffSessionInfo> weiboSession = CheckWeiboSessionAvailableJob.weiboSessionMap.get(tenantUn);
                StaffSessionInfo session_info = null;
                if (!weiboSession.containsKey(user_id)) {
                    
                    for (Staff s : MessageRouter.mulClientStaffMap.get(tenantUn).values()) {
                        List<StaffSessionInfo> session_list = s.getSessionChannelList();
                        for (StaffSessionInfo staffSessionInfo : session_list) {
                            if (!staffSessionInfo.isBusy()) {
                                session_info = staffSessionInfo;
                                session_info.setClient_openid(user_id);
                                session_info.setClient_name(user_name);
                                session_info.setClient_type("wb");
                                session_info.setClient_image(user_image_url);
                                session_info.setLastReceived(new Date());
                                session_info.setSession(UUID.randomUUID().toString());
                                session_info.setBeginTime(API.TIME_FORMAT.format(new Date()));
                                session_info.setBusy(true);
                                break;
                            }
                        }
                        if (session_info != null) {
                            // 首次建立微博--微信会话通道
                            weiboSession.put(user_id, session_info);
                            String token = MessageRouter.getAccessToken(session_info.getAccount());
                            String text = String.format("您已经和微博用户\"%s\"建立了会话连接.\n%s:%s", user_name, user_name, content); 
                            MessageRouter.sendMessage(session_info.getOpenid(), token, text, API.TEXT_MESSAGE);
                            log.info("Weibo session info: " + session_info.toString());
                            return WeChatHttpsUtil.getErrorMsg(0, "Message sended.").toString();
                        } else {
                            LeavingMessageClient c = getLeavingMessageClient(tenantUn, user_id, user_name, user_image_url);
                            c.setSource("wb");
                            recordLeaveMessage(c, tenantUn, content, msgtype);
                            return WeChatHttpsUtil.getErrorMsg(0, "Message recorded.").toString();
                        }
                    }
                } else {
                    session_info = weiboSession.get(user_id);
                }
                
                if (session_info != null) {
                    String token = MessageRouter.getAccessToken(session_info.getAccount());
                    String wx_content = String.format("%s:%s", user_name, content);
                    MessageRouter.sendMessage(session_info.getOpenid(), token, wx_content, msgtype);
                    recordMessage(session_info, content, msgtype);
                    session_info.setLastReceived(new Date());
                }
                
            }
            return WeChatHttpsUtil.getErrorMsg(0, "Message sended.").toString();
        } catch (Exception e) {
            e.printStackTrace();
            return WeChatHttpsUtil.getErrorMsg(20001, e.toString()).toString();
        }
    }
    
    @ResponseBody
    @RequestMapping(value = "/{source}/message", method = RequestMethod.POST)
    public String multiChannelMessage(@PathVariable String source, HttpServletRequest request) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JSONObject json = (JSONObject) JSONSerializer.toJSON(mapper
                    .readValue(request.getInputStream(), Map.class));
            String tenantUn = json.getString("tenantUn");
            String user_id = json.getString("user_id");
            String user_name = json.getString("user_name");
            String content = json.getString("content");
            String msgtype = json.getString("msgtype");
            String user_image_url = json.getString("user_image_url");
            LeavingMessageClient c = new LeavingMessageClient("", user_id, user_name, user_image_url, source);
            recordLeaveMessage(c, tenantUn, content, msgtype);
            return WeChatHttpsUtil.getErrorMsg(0, "Message recorded.").toString();
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.toString());
            return WeChatHttpsUtil.getErrorMsg(20002, e.toString()).toString();
        }
    }
    
    private void recordMessage(StaffSessionInfo s, String content, String msgtype) {
        JSONObject messageToRecord = new JSONObject();
        messageToRecord.put("session_id", s.getSession());
        messageToRecord.put("content", content);
        messageToRecord.put("message_type", msgtype);
        messageToRecord.put("message_source", "wb");
        messageToRecord.put("tenant_code", s.getTenantUn());
        messageToRecord.put("time", API.TIME_FORMAT.format(new Date()));
        messageToRecord.put("sender_openid", s.getClient_openid());
        messageToRecord.put("sender_name", s.getClient_name());
        messageToRecord.put("sender_type", "client");
        messageToRecord.put("sender_public_account", "");
        messageToRecord.put("receiver_openid", s.getOpenid());
        messageToRecord.put("receiver_name", s.getName());
        messageToRecord.put("receiver_type", "staff");
        messageToRecord.put("receiver_public_account", s.getAccount());
        
        JSONObject json_request = new JSONObject();
        json_request.put("session", "hold");
//        json_request.put("module_name", "save_chat_history");
        json_request.put("name_value_list", messageToRecord.toString());
        json_request.put("method", "saveChatHistory");
        
        try {
            MessageExecutor.suaRequestToExecuteQueue.put(json_request);
        } catch (InterruptedException e) {
            log.error("Put message to record queue failed: " + e.toString());
            e.printStackTrace();
        }
    }
    
    private void recordLeaveMessage(LeavingMessageClient c, String tenantUn, String content, String msgtype) {
        JSONObject messageToLeave = new JSONObject();
        messageToLeave.put("tenant_code", tenantUn);
        messageToLeave.put("messager_id", c.getOpenid());
        messageToLeave.put("messager_name", c.getName());
        messageToLeave.put("messager_photo", c.getHeadimgurl());
        messageToLeave.put("messager_public_account", "");
        messageToLeave.put("content", content);
        messageToLeave.put("time", API.TIME_FORMAT.format(new Date()));
        messageToLeave.put("message_status", "0");
        messageToLeave.put("type", msgtype);
        messageToLeave.put("source", c.getSource());
        messageToLeave.put("message_group_id", c.getUuid());
        
        JSONObject json_request = new JSONObject();
        json_request.put("session", "hold");
        json_request.put("name_value_list", messageToLeave.toString());
        json_request.put("method", "saveChatMessage");
        
        try {
            MessageExecutor.suaRequestToExecuteQueue.put(json_request);
        } catch (InterruptedException e) {
            log.error("Put leaving message to record queue failed: " + e.toString());
            e.printStackTrace();
        }
    }
    
    private LeavingMessageClient getLeavingMessageClient(String tenantUn, String user_id, String user_name, String user_image_url) {
        log.info("Before: "+ CheckLeavingMessageJob.leavingMessageClientList.toString());
        Map<String, LeavingMessageClient> c_map = CheckLeavingMessageJob.leavingMessageClientList.get(tenantUn);
        LeavingMessageClient c = null;
        if (null == c_map) {
            c_map = new HashMap<String, LeavingMessageClient>();
            c = new LeavingMessageClient("", user_id, user_name, user_image_url, "wb");
            c_map.put(user_id, c);
            CheckLeavingMessageJob.leavingMessageClientList.put(tenantUn, c_map);
        } else {
            c = c_map.get(user_id);
            if (null == c) {
                c = new LeavingMessageClient("", user_id, user_name, user_image_url, "wb");
                c_map.put(user_id, c);
            } 
//            else {
//                c.setDate(new Date().getTime());
//            }
        }
        log.info("After: "+ CheckLeavingMessageJob.leavingMessageClientList.toString());
        return c;
    }
}
