package com.hesong.weixinAPI.job;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import redis.clients.jedis.Jedis;

import com.hesong.weixinAPI.context.ContextPreloader;
import com.hesong.weixinAPI.core.MessageRouter;
import com.hesong.weixinAPI.model.WaitingClient;
import com.hesong.weixinAPI.tools.API;
import com.hesong.weixinAPI.tools.RedisOperations;

public class CheckWaitingListJob implements Job {

    public static final String WAITING_LIST_GROUP = "CheckWaitingListJob";

    private static long default_waiting_duration = 60000l;

    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        Jedis jedis = null;
        try {
            jedis = ContextPreloader.jedisPool.getResource();
            
            for (String tenantUn : MessageRouter.waitingList.keySet()) {
                Map<String, List<WaitingClient>> waitingClients = MessageRouter.waitingList
                        .get(tenantUn);
                long timeout;
                if (jedis.hexists(API.REDIS_TENANT_WAITING_DURATION, tenantUn)) {
                    timeout = Long.parseLong(jedis.hget(API.REDIS_TENANT_WAITING_DURATION, tenantUn));
                } else {
                    timeout = default_waiting_duration;
                }
                
                for (String category : waitingClients.keySet()) {
                    List<WaitingClient> clients = waitingClients.get(category);
                    for (WaitingClient waitingClient : clients) {
                        if ((new Date().getTime() - waitingClient.getTime()) >= timeout) {
                            String text = "系统消息:客服妹子繁忙,请稍后再试或者使用在线留言,我们将在尽快回复您[微笑]";
                            MessageRouter.sendMessage(
                                    waitingClient.getOpenid(), MessageRouter
                                            .getAccessToken(waitingClient
                                                    .getAccount()), text,
                                    API.TEXT_MESSAGE);
                            MessageRouter.waitingListIDs.remove(waitingClient
                                    .getOpenid());
                            clients.remove(waitingClient);
                            RedisOperations.decrWaitingListCount(tenantUn);
                        }
                    }
                    if (clients.isEmpty()) {
                        waitingClients.remove(category);
                    }
                }

                if (waitingClients.isEmpty()) {
                    MessageRouter.waitingList.remove(tenantUn);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ContextPreloader.jedisPool.returnResource(jedis);
        }

    }

}
