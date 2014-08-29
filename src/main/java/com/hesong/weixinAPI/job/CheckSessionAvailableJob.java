package com.hesong.weixinAPI.job;

import java.util.Date;
import java.util.HashMap;
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
    
    /**
     * <tenantUn, <client_openid, session>>
     */
    public static ConcurrentMap<String, Map<String, StaffSessionInfo>> sessionMap = new ConcurrentHashMap<String, Map<String, StaffSessionInfo>>();

    private static long default_session_available_duration = 120000l;

    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        Jedis jedis = null;
        try {
            jedis = ContextPreloader.jedisPool.getResource();
            for (String tenantUn : sessionMap.keySet()) {
                Map<String, StaffSessionInfo> client_session = sessionMap
                        .get(tenantUn);
                long timeout;
                if (jedis.hexists(API.REDIS_TENANT_SESSION_AVAILABLE_DURATION, tenantUn)) {
                    timeout = Long.parseLong(jedis.hget(API.REDIS_TENANT_SESSION_AVAILABLE_DURATION, tenantUn));
                } else {
                    timeout = default_session_available_duration;
                }
                
                for (String client_openid : client_session.keySet()) {

                    StaffSessionInfo session = client_session.get(client_openid);
                    if (session != null
                            && (new Date().getTime() - session.getLastReceived()
                                    .getTime()) > timeout) {
                        log.info("Time out, remove client.");
                        
                        String cToken = MessageRouter.getAccessToken(session.getClient_account(), jedis);
                        
                        try {
                            
                            int endtime;
                            if (jedis.hexists(API.REDIS_TENANT_END_SESSION_DURATION_DURATION, tenantUn)) {
                                endtime = Integer.parseInt(jedis.hget(API.REDIS_TENANT_END_SESSION_DURATION_DURATION, tenantUn)) / 1000;
                            } else {
                                endtime = 30;
                            }

                            String text = String.format(ContextPreloader.messageProp.getProperty("client.message.remaindToActiveSession"), endtime);
                            // To client
                            if (session.getClient_type().equalsIgnoreCase("wx")) {
                                MessageRouter.sendMessage(client_openid, cToken,
                                        text, API.TEXT_MESSAGE);
                            }
                            
                            session.setLastReceived(new Date());
                            if (CheckEndSessionJob.endSessionMap.containsKey(tenantUn)) {
                                CheckEndSessionJob.endSessionMap.get(tenantUn).put(session.getClient_openid(), session);
                            } else {
                                CheckEndSessionJob.endSessionMap.put(tenantUn, new HashMap<String, StaffSessionInfo>());
                                CheckEndSessionJob.endSessionMap.get(tenantUn).put(session.getClient_openid(), session);
                            }
                        } catch (Exception e) {
                            log.error("Send message error: " + e.toString());
                        }

                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ContextPreloader.jedisPool.returnResource(jedis);
        }


    }

}
