package com.hesong.weChatAdapter.context;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;

import com.hesong.weChatAdapter.model.AccessToken;
import com.hesong.weChatAdapter.tools.WeChatHttpsUtil;


@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class UpdateAccessTokenJob implements Job {

    public static final String JOB_GROUP = "UpdateAccessToken";

    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {

        for(String key: ContextPreloader.Account_Map.keySet()){
            AccessToken ac = ContextPreloader.Account_Map.get(key);
            ac = WeChatHttpsUtil.getAccessToken(ac.getAppid(), ac.getAppSecret());
            ContextPreloader.ContextLog.info("Access token updated for account: "+key);
            if (ac == null) {
                ContextPreloader.ContextLog.error("Get Access_Token failed for account: "+key);
                continue;
            }
            ContextPreloader.Account_Map.put(key, ac);
        }

        
    }
}