package com.hesong.weixinAPI.job;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.hesong.weixinAPI.context.ContextPreloader;
import com.hesong.weixinAPI.core.MessageExecutor;
import com.hesong.weixinAPI.model.LeavingMessageClient;

public class CheckLeavingMessageJob implements Job {

    public static final String CHECK_LEAVING_MESSAGE_GROUP = "CheckLeavingMessageJob";
    public static Map<String, LeavingMessageClient> leavingMessageClientList = new HashMap<String, LeavingMessageClient>();
    private static long levesMessageDuration = 300000l;
    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        long now = new Date().getTime();
        String text = "时间到啦[微笑],您的留言已被系统记录,客服MM将在第一时间给您回复,如果您想继续留言请点击在线留言按钮,感谢您的支持!";
        for (LeavingMessageClient c : leavingMessageClientList.values()) {
            if ((now - c.getDate()) > levesMessageDuration) {
                String token = ContextPreloader.Account_Map.get(c.getAccount()).getToken();
                MessageExecutor.sendMessage(c.getOpenid(), token, text, "text");
                leavingMessageClientList.remove(c.getOpenid());
            }
        }
    }

}
