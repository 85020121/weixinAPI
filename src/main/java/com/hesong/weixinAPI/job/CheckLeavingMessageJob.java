package com.hesong.weixinAPI.job;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.hesong.weixinAPI.core.MessageExecutor;
import com.hesong.weixinAPI.core.MessageRouter;
import com.hesong.weixinAPI.model.LeavingMessageClient;

public class CheckLeavingMessageJob implements Job {

    public static final String CHECK_LEAVING_MESSAGE_GROUP = "CheckLeavingMessageJob";
    public static Map<String, Map<String, LeavingMessageClient>> leavingMessageClientList = new HashMap<String, Map<String, LeavingMessageClient>>();
    private static long levesMessageDuration = 300000l;
    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        long now = new Date().getTime();
        String text = "时间到啦[微笑],您的留言已被系统记录,客服MM将在第一时间给您回复,如果您想继续留言请点击在线留言按钮,感谢您的支持!";
        for (String tenantUn : leavingMessageClientList.keySet()) {
            Map<String, LeavingMessageClient> c_map = leavingMessageClientList.get(tenantUn);
            for (LeavingMessageClient c : c_map.values()) {
                if ((now - c.getDate()) > levesMessageDuration) {
                    if (c.getSource().equalsIgnoreCase("wx")) {
                        String token = MessageRouter.getAccessToken(c.getAccount());
                        MessageExecutor.sendMessage(c.getOpenid(), token, text, "text");
                        MessageRouter.newMessageRemaind(tenantUn);
                    }
                    c_map.remove(c.getOpenid());
                }
            }
            if (c_map.isEmpty()) {
                leavingMessageClientList.remove(tenantUn);
            }
        }
    }

}
