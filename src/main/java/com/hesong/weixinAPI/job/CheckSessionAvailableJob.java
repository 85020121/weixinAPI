package com.hesong.weixinAPI.job;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import redis.clients.jedis.Jedis;

import com.hesong.weixinAPI.context.ContextPreloader;
import com.hesong.weixinAPI.core.MessageRouter;
import com.hesong.weixinAPI.model.StaffSessionInfo;
import com.hesong.weixinAPI.tools.API;

public class CheckSessionAvailableJob implements Job {

    private static Logger log = Logger
            .getLogger(CheckSessionAvailableJob.class);

    public static final String CHECK_SESSION_GROUP = "CheckSessionAvailableJob";
    public static final Map<String, Long> session_available_duration_map = new ConcurrentHashMap<String, Long>();
    
    /**
     * <tenantUn, <client_openid, session>>
     */
    public static ConcurrentMap<String, Map<String, StaffSessionInfo>> sessionMap = new ConcurrentHashMap<String, Map<String, StaffSessionInfo>>();

    private static long session_available_duration = 120000l;

    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        for (String tenantUn : sessionMap.keySet()) {
            Map<String, StaffSessionInfo> client_session = sessionMap
                    .get(tenantUn);
            long timeout;
            if (session_available_duration_map.containsKey(tenantUn)) {
                timeout = session_available_duration_map.get(tenantUn);
            } else {
                timeout = session_available_duration;
            }
            
            for (String client_openid : client_session.keySet()) {

                StaffSessionInfo session = client_session.get(client_openid);
                if (session != null
                        && (new Date().getTime() - session.getLastReceived()
                                .getTime()) > timeout) {
                    log.info("Time out, remove client.");
                    
                    Jedis jedis = ContextPreloader.jedisPool.getResource();
                    
                    String cToken = MessageRouter.getAccessToken(session.getClient_account(), jedis);
                    String sToken = MessageRouter.getAccessToken(session.getAccount(), jedis);
                    
                    ContextPreloader.jedisPool.returnResource(jedis);
                    
                    try {

                        String text = "系统提示：会话超时，您已退出人工对话，感谢您的使用。";
                        // To client
                        if (session.getClient_type().equalsIgnoreCase("wx")) {
                            MessageRouter.sendMessage(client_openid, cToken,
                                    text, API.TEXT_MESSAGE);
                        }

                        // To staff
                        text = "系统提示：会话超时，该会话已结束。";
                        MessageRouter.sendMessage(session.getOpenid(), sToken,
                                text, API.TEXT_MESSAGE);
                        // To web staff
                        if (session.isWebStaff()) {
                            MessageRouter.sendWebMessage("sysMessage", text,
                                    session.getOpenid(), "",
                                    session.getStaff_uuid());
                        }

                    } catch (Exception e) {
                        log.error("Send message error: " + e.toString());
                    }

                    session.setBusy(false);
                    session.setEndTime(API.TIME_FORMAT.format(new Date()));
                    MessageRouter.recordSession(session, 0);
                    
                    session.setClient_openid("");
                    session.setClient_account("");

                    client_session.remove(client_openid);
                }
            }
            
            if (client_session.isEmpty()) {
                sessionMap.remove(tenantUn);
            }
        }

    }

}
