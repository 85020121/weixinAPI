package com.hesong.weixinAPI.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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

import com.hesong.weixinAPI.context.ContextPreloader;
import com.hesong.weixinAPI.core.MessageRouter;
import com.hesong.weixinAPI.job.CheckSessionAvailableJob;
import com.hesong.weixinAPI.model.Staff;
import com.hesong.weixinAPI.model.StaffSessionInfo;
import com.hesong.weixinAPI.tools.WeChatHttpsUtil;

@Controller
@RequestMapping("/staff")
public class StaffController {
    
    private static Logger log = Logger.getLogger(StaffController.class);
    
    private static String SEND_MESSAGE_REQUEST_URL = "https://api.weixin.qq.com/cgi-bin/message/custom/send?access_token=";

    @ResponseBody
    @RequestMapping(value = "/takeSession", method = RequestMethod.GET)
    public String takeSession(HttpServletRequest request) {
        String client_openid = request.getParameter("clientid");
        String client_account = request.getParameter("account");
        String client_name = request.getParameter("clientname");
        String staff_id = request.getParameter("staffid");
        if (client_openid == null || client_account == null || staff_id == null) {
            return "error";
        }
        
        log.info(client_openid+ "  "+ client_account + "  " + staff_id);
        
        synchronized (this) {
            if (CheckSessionAvailableJob.clientMap.get(client_openid) == null) {
                CheckSessionAvailableJob.clientMap.put(client_openid, client_account);
                log.info("Staff map: " + MessageRouter.activeStaffMap.toString());
                StaffSessionInfo s = MessageRouter.activeStaffMap.get(staff_id);
                if (s == null) {
                    return "Null staff error.";
                }
                s.setBusy(true);
                s.setLastReceived(new Date());
                s.setClient_openid(client_openid);
                s.setClient_account(client_account);
                s.setClient_name(client_name);
                CheckSessionAvailableJob.sessionMap.put(client_openid, s);
                
                // Remind client
                JSONObject message = new JSONObject();
                message.put("msgtype", "text");
                message.put("touser", client_openid);
                JSONObject content = new JSONObject();
                content.put("content", "会话已建立,客服"+s.getStaffid()+"将为您服务[微笑]");
                message.put("text", content);
                String url = SEND_MESSAGE_REQUEST_URL + ContextPreloader.Account_Map.get(client_account).getToken();
                JSONObject ret = WeChatHttpsUtil.httpsRequest(url, "POST", message.toString());
                log.info("Send message ret: "+ret.toString());
                
            } else {
                return "This session has been taken by someone else, go back。";
            }
        }
        return "Build session success, go back!";
    }
    
    @ResponseBody
    @RequestMapping(value = "/{openid}/checkin/{staffid}", method = RequestMethod.GET)
    public int checkin_old(@PathVariable String openid, @PathVariable String staffid, HttpServletRequest request) {
        log.info("Checked in: " + openid + " staff_id: " + staffid);
        String account = "gh_0221936c0c16";
        StaffSessionInfo s = new StaffSessionInfo(account, openid, staffid, "");
        MessageRouter.activeStaffMap.put(openid, s);
        String text = "登入成功";
        String token = ContextPreloader.Account_Map.get(account).getToken();
        JSONObject message = new JSONObject();
        message.put("msgtype", "text");
        message.put("touser", openid);
        JSONObject content = new JSONObject();
        content.put("content", text);
        message.put("text", content);
        try {
            String url = SEND_MESSAGE_REQUEST_URL + token;
            JSONObject ret = WeChatHttpsUtil.httpsRequest(url, "POST",
                    message.toString());
            log.info("Send message ret: " + ret.toString());
        } catch (Exception e) {
            log.error("Send message error: " + e.toString());
        }
        log.info("Add staff to list: " + s.toString());
        return 200;
    }
    
    @ResponseBody
    @RequestMapping(value = "/{staff_uuid}/checkin", method = RequestMethod.GET)
    public int checkin(@PathVariable String openid, @PathVariable String staff_uuid, HttpServletRequest request) {
        log.info("Checked in staff_id: " + staff_uuid);
        ObjectMapper mapper = new ObjectMapper();
        try {
            JSONObject staff_info = (JSONObject) JSONSerializer.toJSON(mapper
                    .readValue(request.getInputStream(), Map.class));
            String account = staff_info.getString("account");
            
            Map<String, Staff> staff_map = null;
            if (MessageRouter.mulClientStaffMap.containsKey(account)) {
                staff_map = MessageRouter.mulClientStaffMap.get(account);
                if (staff_map.containsKey(staff_uuid)) {
                    log.info("Staff already checked in, working num: " + staff_uuid);
                    return 200;
                }
            } else {
                staff_map = new HashMap<String, Staff>();
                MessageRouter.mulClientStaffMap.put(account, staff_map);
            }
            
            // Create staff
            String staff_working_num = staff_info.getString("staffid");
            String staff_name = staff_info.getString("staff_name");
            JSONArray channel_list = staff_info.getJSONArray("channels");
            List<StaffSessionInfo> sessionChannelList = new ArrayList<StaffSessionInfo>();
            for (int i = 0; i < channel_list.size(); i++) {
                JSONObject channel = channel_list.getJSONObject(i);
                String staff_account = channel.getString("account");
                String staff_openid = channel.getString("openid");
                StaffSessionInfo s = new StaffSessionInfo(staff_account, staff_openid, staff_working_num, staff_name);
                sessionChannelList.add(s);
            }
            Staff staff = new Staff(staff_uuid, staff_name, account, staff_working_num, sessionChannelList);
            log.info("Staff checked in: " + staff.toString());
            return 200;
        } catch (Exception e) {
            log.error("Staff checkin failed, staff_uuid = " + staff_uuid + ", caused by: " + e.toString());
            return 404;
        }
        
    }
}
