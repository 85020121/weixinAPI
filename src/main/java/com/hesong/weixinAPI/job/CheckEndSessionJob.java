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
        Jedis jedis = null;
        try {
            jedis = ContextPreloader.jedisPool.getResource();
            for (String tenantUn : endSessionMap.keySet()) {
                
                long timeout;
                if (jedis.hexists(API.REDIS_TENANT_END_SESSION_DURATION_DURATION, tenantUn)) {
                    timeout = Long.parseLong(jedis.hget(API.REDIS_TENANT_END_SESSION_DURATION_DURATION, tenantUn));
                } else {
                    timeout = session_waiting_duration;
                }
                
                Map<String, StaffSessionInfo> client_session = endSessionMap
                        .get(tenantUn);
                for (String client_openid : client_session.keySet()) {

                    StaffSessionInfo session = client_session
                            .get(client_openid);
                    if (session != null
                            && (new Date().getTime() - session
                                    .getLastReceived().getTime()) > timeout) {
                        log.info("Time out, remove client.");

                        jedis = ContextPreloader.jedisPool.getResource();

                        String cToken = MessageRouter.getAccessToken(
                                session.getClient_account(), jedis);
                        String sToken = MessageRouter.getAccessToken(
                                session.getAccount(), jedis);

                        try {

                            String text = "系统提示：会话结束，您已退出人工对话，感谢您的使用。";
                            // To client
                            if (session.getClient_type().equalsIgnoreCase("wx")) {
                                MessageRouter.sendMessage(client_openid,
                                        cToken, text, API.TEXT_MESSAGE);
                            }

                            // To staff
                            text = "系统提示：该会话已结束。";
                            MessageRouter.sendMessage(session.getOpenid(),
                                    sToken, text, API.TEXT_MESSAGE);
                            // To web staff
                            if (session.isWebStaff()) {
                                MessageRouter.sendWebMessage("sysMessage",
                                        text, session.getOpenid(), "",
                                        session.getStaff_uuid(), "endSession");
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
                        CheckSessionAvailableJob.sessionMap.get(tenantUn)
                                .remove(client_openid);
                        
                        // Remaind staff
                        int waiting_count = MessageRouter.getWaitingClientCount(tenantUn);
                        if (waiting_count > 0) {
                            String text = String.format("系统提示：有%d个客户在等待人工服务，请点击抢接接入客户。", waiting_count);
                            MessageRouter.sendMessage(session.getOpenid(), sToken, text, API.TEXT_MESSAGE);
                        }
                    }
                }

                if (client_session.isEmpty()) {
                    endSessionMap.remove(tenantUn);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ContextPreloader.jedisPool.returnResource(jedis);
        }

    }

}
