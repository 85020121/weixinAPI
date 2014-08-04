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

public class CheckEndSessionJob implements Job {
    
    private static Logger log = Logger.getLogger(CheckEndSessionJob.class);

    public static final String END_SESSION_GROUP = "CheckEndSessionJob";
    
    /**
     * <tenantUn, <client_openid, session>>
     */
    public static ConcurrentMap<String, Map<String, StaffSessionInfo>> endSessionMap = new ConcurrentHashMap<String, Map<String, StaffSessionInfo>>();

    private static long session_waiting_duration = 30000l;

    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        for (String tenantUn : endSessionMap.keySet()) {
            Map<String, StaffSessionInfo> client_session = endSessionMap
                    .get(tenantUn);
            for (String client_openid : client_session.keySet()) {

                StaffSessionInfo session = client_session.get(client_openid);
                if (session != null
                        && (new Date().getTime() - session.getLastReceived()
                                .getTime()) > session_waiting_duration) {
                    log.info("Time out, remove client.");

                    Jedis jedis = ContextPreloader.jedisPool.getResource();
                    
                    String cToken = MessageRouter.getAccessToken(session.getClient_account(), jedis);
                    String sToken = MessageRouter.getAccessToken(session.getAccount(), jedis);
                    
                    ContextPreloader.jedisPool.returnResource(jedis);
                    
                    try {

                        String text = "系统消息:会话结束,您已退出人工对话，感谢您的使用。";
                        // To client
                        if (session.getClient_type().equalsIgnoreCase("wx")) {
                            MessageRouter.sendMessage(client_openid, cToken,
                                    text, API.TEXT_MESSAGE);
                        }

                        // To staff
                        text = "系统消息:该会话已结束。";
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

                    client_session.remove(client_openid);
                    CheckSessionAvailableJob.sessionMap.get(tenantUn).remove(client_openid);
                }
            }
            
            if (client_session.isEmpty()) {
                endSessionMap.remove(tenantUn);
            }
        }

    }
        

}
