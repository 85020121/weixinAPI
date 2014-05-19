package com.hesong.weixinAPI.job;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.hesong.weixinAPI.model.StaffSessionInfo;

public class CheckSessionAvailableJob implements Job {
    
    private static Logger log = Logger.getLogger(CheckSessionAvailableJob.class);
    
    public static final String CHECK_SESSION_GROUP = "CheckSessionAvailableJob";

    public static ConcurrentMap<String, String> clientMap = new ConcurrentHashMap<String, String>();
    public static ConcurrentMap<String, StaffSessionInfo> sessionMap = new ConcurrentHashMap<String, StaffSessionInfo>();
    
    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        log.info("Check");
        for (String client_openid : sessionMap.keySet()) {
            StaffSessionInfo staff = sessionMap.get(client_openid);
            if ((new Date().getTime() - staff.getLastReceived().getTime()) > 20000) {
                log.info("Time out, remove client.");
                clientMap.remove(client_openid);
                sessionMap.remove(client_openid);
            }
        }
        
    }

}
