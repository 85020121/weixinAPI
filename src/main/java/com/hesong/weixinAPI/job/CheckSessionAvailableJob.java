package com.hesong.weixinAPI.job;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.hesong.weixinAPI.context.ContextPreloader;
import com.hesong.weixinAPI.model.StaffSessionInfo;
import com.hesong.weixinAPI.tools.WeChatHttpsUtil;

public class CheckSessionAvailableJob implements Job {
    
    private static Logger log = Logger.getLogger(CheckSessionAvailableJob.class);
    
    public static final String CHECK_SESSION_GROUP = "CheckSessionAvailableJob";
    private static String SEND_MESSAGE_REQUEST_URL = "https://api.weixin.qq.com/cgi-bin/message/custom/send?access_token=";

    public static ConcurrentMap<String, String> clientMap = new ConcurrentHashMap<String, String>();
    public static ConcurrentMap<String, StaffSessionInfo> sessionMap = new ConcurrentHashMap<String, StaffSessionInfo>();
    
    private static long session_available_duration = 120000;
    
    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        for (String client_openid : sessionMap.keySet()) {
            StaffSessionInfo staff = sessionMap.get(client_openid);
            if (staff != null && (new Date().getTime() - staff.getLastReceived().getTime()) > session_available_duration) {
                log.info("Time out, remove client.");
                
                JSONObject message = new JSONObject();
                message.put("msgtype", "text");
                message.put("touser", client_openid);
                JSONObject content = new JSONObject();
                content.put("content", "系统消息:会话超时,您已退出人工对话，感谢您的使用。");
                message.put("text", content);
                String cToken = ContextPreloader.Account_Map.get(clientMap.get(client_openid)).getToken();
                String sToken = ContextPreloader.Account_Map.get(staff.getAccount()).getToken();
                try {
                    String request = null;
                    JSONObject ret = null;
                    
                    // To client
                    if (staff.getClient_type().equalsIgnoreCase("wx")) {
                        request = SEND_MESSAGE_REQUEST_URL + cToken;
                        ret = WeChatHttpsUtil.httpsRequest(request, "POST", message.toString());
                        log.info("Send message ret: "+ret.toString());
                    }

                    // To staff
                    request = SEND_MESSAGE_REQUEST_URL + sToken;
                    message.put("touser", staff.getOpenid());
                    content.put("content", "会话超时,该会话已结束。");
                    message.put("text", content);
                    ret = WeChatHttpsUtil.httpsRequest(request, "POST", message.toString());
                    log.info("Send message ret: "+ret.toString());
                    
                } catch (Exception e) {
                    log.error("Send message error: "+e.toString());
                }
                
                staff.setBusy(false);
                clientMap.remove(client_openid);
                sessionMap.remove(client_openid);
            }
        }
        
    }

}
