package com.hesong.weixinAPI.job;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.hesong.weixinAPI.context.ContextPreloader;
import com.hesong.weixinAPI.core.MessageRouter;
import com.hesong.weixinAPI.model.StaffSessionInfo;
import com.hesong.weixinAPI.tools.API;

public class CheckSessionAvailableJob implements Job {
    
    private static Logger log = Logger.getLogger(CheckSessionAvailableJob.class);
    
    public static final String CHECK_SESSION_GROUP = "CheckSessionAvailableJob";

    public static ConcurrentMap<String, String> clientMap = new ConcurrentHashMap<String, String>();
    public static ConcurrentMap<String, StaffSessionInfo> sessionMap = new ConcurrentHashMap<String, StaffSessionInfo>();
    
    private static long session_available_duration = 300000l;
    
    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        for (String client_openid : sessionMap.keySet()) {
            StaffSessionInfo session = sessionMap.get(client_openid);
            if (session != null && (new Date().getTime() - session.getLastReceived().getTime()) > session_available_duration) {
                log.info("Time out, remove client.");
                
                String cToken = ContextPreloader.Account_Map.get(session.getClient_account()).getToken();
                String sToken = ContextPreloader.Account_Map.get(session.getAccount()).getToken();
                try {
                    
                    String text = "系统消息:会话超时,您已退出人工对话，感谢您的使用。";
                    // To client
                    if (session.getClient_type().equalsIgnoreCase("wx")) {
                        MessageRouter.sendMessage(client_openid, cToken, text, API.TEXT_MESSAGE);
                    }

                    // To staff
                    text = "系统消息:会话超时,该会话已结束。";
                    MessageRouter.sendMessage(session.getOpenid(), sToken, text, API.TEXT_MESSAGE);
                    
                } catch (Exception e) {
                    log.error("Send message error: "+e.toString());
                }
                
                session.setBusy(false);
                session.setEndTime(API.TIME_FORMAT.format(new Date()));
                MessageRouter.recordSession(session, 0);
                
                clientMap.remove(client_openid);
                sessionMap.remove(client_openid);
            }
        }
        
    }

}
