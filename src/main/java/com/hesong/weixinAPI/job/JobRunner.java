package com.hesong.weixinAPI.job;

import java.io.IOException;

import static org.quartz.TriggerBuilder.*;
import static org.quartz.CronScheduleBuilder.*;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import com.hesong.weixinAPI.context.ContextPreloader;

/**
 * 抓取邮件流程触发器
 * 
 * @author Bowen
 * 
 */
public class JobRunner {

    public static Scheduler scheduler;

    public void task() throws IOException, SchedulerException {

        // Update access token job
        String cronExpression = "0 0 0/1 * * ?";

        JobDetail job = JobBuilder
                .newJob(UpdateAccessTokenJob.class)
                .withIdentity("UpdateAccessTokenJob",
                        UpdateAccessTokenJob.JOB_GROUP).build();
        Trigger trigger = newTrigger()
                .withIdentity("UpdateAccessTokenJob",
                        UpdateAccessTokenJob.JOB_GROUP)
                .withSchedule(cronSchedule(cronExpression)).forJob(job).build();

        scheduler = new StdSchedulerFactory("quartz.properties").getScheduler();
        
        // Check Weixin Session Available Job
        String checkCronExpression = "0/30 * * * * ?";
        JobDetail session_job = JobBuilder
                .newJob(CheckSessionAvailableJob.class)
                .withIdentity("CheckSessionAvailableJob",
                        CheckSessionAvailableJob.CHECK_SESSION_GROUP).build();
        Trigger session_trigger = newTrigger()
                .withIdentity("CheckSessionAvailableJob",
                        CheckSessionAvailableJob.CHECK_SESSION_GROUP)
                .withSchedule(cronSchedule(checkCronExpression)).forJob(session_job).build();
        
        // Check Weibo Session Available Job
        JobDetail weibo_session_job = JobBuilder
                .newJob(CheckWeiboSessionAvailableJob.class)
                .withIdentity("CheckWeiboSessionAvailableJob",
                        CheckWeiboSessionAvailableJob.CHECK_SESSION_GROUP).build();
        Trigger weibo_session_trigger = newTrigger()
                .withIdentity("CheckWeiboSessionAvailableJob",
                        CheckWeiboSessionAvailableJob.CHECK_SESSION_GROUP)
                .withSchedule(cronSchedule(checkCronExpression)).forJob(weibo_session_job).build();
        
        // Check Leaving Message Duration Job
        String checkLeaveMessageCronExpression = "0/30 * * * * ?";
        JobDetail leave_message_job = JobBuilder
                .newJob(CheckLeavingMessageJob.class)
                .withIdentity("CheckLeavingMessageJob",
                        CheckLeavingMessageJob.CHECK_LEAVING_MESSAGE_GROUP).build();
        Trigger leave_message_trigger = newTrigger()
                .withIdentity("CheckLeavingMessageJob",
                        CheckLeavingMessageJob.CHECK_LEAVING_MESSAGE_GROUP)
                .withSchedule(cronSchedule(checkLeaveMessageCronExpression)).forJob(leave_message_job).build();
        
        // Get Leaved Message Job
        String getLeavedMessageCronExpression = "0/30 * * * * ?";
        JobDetail get_leaved_message_job = JobBuilder
                .newJob(GetLeavedMessageJob.class)
                .withIdentity("GetLeavedMessageJob",
                        GetLeavedMessageJob.GET_LEAVED_MESSAGE_GROUP).build();
        Trigger get_leaved_message_trigger = newTrigger()
                .withIdentity("GetLeavedMessageJob",
                        GetLeavedMessageJob.GET_LEAVED_MESSAGE_GROUP)
                .withSchedule(cronSchedule(getLeavedMessageCronExpression)).forJob(get_leaved_message_job).build();
        
        // Check Weixin End Session Job
        String endSessionCronExpression = "0/10 * * * * ?";
        JobDetail end_session_job = JobBuilder
                .newJob(CheckEndSessionJob.class)
                .withIdentity("CheckEndSessionJob",
                        CheckEndSessionJob.END_SESSION_GROUP).build();
        Trigger end_session_trigger = newTrigger()
                .withIdentity("CheckEndSessionJob",
                        CheckEndSessionJob.END_SESSION_GROUP)
                .withSchedule(cronSchedule(endSessionCronExpression)).forJob(end_session_job).build();
        
        // Check Waiting list Job
        String waitingListCronExpression = "0/20 * * * * ?";
        JobDetail waitingList_job = JobBuilder
                .newJob(CheckWaitingListJob.class)
                .withIdentity("CheckWaitingListJob",
                        CheckWaitingListJob.WAITING_LIST_GROUP).build();
        Trigger waitingList_trigger = newTrigger()
                .withIdentity("CheckWaitingListJob",
                        CheckWaitingListJob.WAITING_LIST_GROUP)
                .withSchedule(cronSchedule(waitingListCronExpression)).forJob(waitingList_job).build();

        scheduler = new StdSchedulerFactory("quartz.properties").getScheduler();
        // Add jobs to scheduler
        scheduler.scheduleJob(job, trigger);
        scheduler.scheduleJob(session_job, session_trigger);
        scheduler.scheduleJob(weibo_session_job, weibo_session_trigger);
        scheduler.scheduleJob(leave_message_job, leave_message_trigger);
        //scheduler.scheduleJob(get_leaved_message_job, get_leaved_message_trigger);
        scheduler.scheduleJob(end_session_job, end_session_trigger);
        scheduler.scheduleJob(waitingList_job, waitingList_trigger);
        
        scheduler.start();
        ContextPreloader.ContextLog.info("Start Job Runner.");
    }

}
