package com.hesong.weixinAPI.job;

import java.util.Date;
import java.util.Map;
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

public class CheckWeiboSessionAvailableJob implements Job{
    private static Logger log = Logger.getLogger(CheckSessionAvailableJob.class);
    
    public static final String CHECK_SESSION_GROUP = "CheckWeiboSessionAvailableJob";

    public static ConcurrentMap<String, Map<String, StaffSessionInfo>> weiboSessionMap = new ConcurrentHashMap<String, Map<String, StaffSessionInfo>>();

    private static long session_available_duration = 300000l;
    
    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        long now = new Date().getTime();
        
        for (String tenantUn : weiboSessionMap.keySet()) {
            Map<String, StaffSessionInfo> session_map = weiboSessionMap.get(tenantUn);
           
            for (String id : session_map.keySet()) {
                StaffSessionInfo session = session_map.get(id);
                if (null != session && (now - session.getLastReceived().getTime() > session_available_duration)) {
                    String token = ContextPreloader.Account_Map.get(session.getAccount()).getToken();
                    String text = "系统消息:会话超时,该会话已结束。";
                    MessageRouter.sendMessage(session.getOpenid(), token, text, API.TEXT_MESSAGE);
                    session_map.remove(session.getClient_openid());
                    log.info("Weibo session removed for client: " + session.getClient_openid());
                    session.setBusy(false);
                    session_map.remove(id);
                }
            }
            if (session_map.isEmpty()) {
                weiboSessionMap.remove(tenantUn);
            }
            
        }
        
    }
}
