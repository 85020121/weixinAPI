package com.hesong.weixinAPI.controller;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hesong.weixinAPI.context.ContextPreloader;
import com.hesong.weixinAPI.core.MessageRouter;
import com.hesong.weixinAPI.job.CheckSessionAvailableJob;
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
    public int checkin(@PathVariable String openid, @PathVariable String staffid, HttpServletRequest request) {
        log.info("Checked in: " + openid + " staff_id: " + staffid);
        String account = "gh_0221936c0c16";
        StaffSessionInfo s = new StaffSessionInfo(account, openid, staffid);
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
}
