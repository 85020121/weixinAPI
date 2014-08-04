package com.hesong.weixinAPI.job;

import java.util.Date;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.hesong.weixinAPI.core.MessageRouter;
import com.hesong.weixinAPI.model.WaitingClient;
import com.hesong.weixinAPI.tools.API;

public class CheckWaitingListJob implements Job {

    public static final String WAITING_LIST_GROUP = "CheckWaitingListJob";
    public static final Map<String, Long> waiting_duration_map = new ConcurrentHashMap<String, Long>();

    private static long waiting_duration = 60000l;

    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        for (String tenantUn : MessageRouter.waitingList.keySet()) {
            Map<String, Queue<WaitingClient>> waitingClients = MessageRouter.waitingList
                    .get(tenantUn);
            long timeout;
            if (waiting_duration_map.containsKey(tenantUn)) {
                timeout = waiting_duration_map.get(tenantUn);
            } else {
                timeout = waiting_duration;
            }
            
            for (String category : waitingClients.keySet()) {
                Queue<WaitingClient> clients = waitingClients.get(category);
                for (WaitingClient waitingClient : clients) {
                    if ((new Date().getTime() - waitingClient.getTime()) >= timeout) {
                        String text = "系统消息:客服妹子繁忙,请稍后再试或者使用在线留言,我们将在尽快回复您[微笑]";
                        MessageRouter.sendMessage(waitingClient.getOpenid(),
                                MessageRouter.getAccessToken(waitingClient
                                        .getAccount()), text, API.TEXT_MESSAGE);
                        MessageRouter.waitingListIDs.remove(waitingClient
                                .getOpenid());
                        clients.remove(waitingClient);
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
    }

}
