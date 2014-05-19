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
        // Add job to scheduler
        scheduler.scheduleJob(job, trigger);
        
        // Check Session Available Job
        String checkCronExpression = "0/20 * * * * ?";
        JobDetail session_job = JobBuilder
                .newJob(UpdateAccessTokenJob.class)
                .withIdentity("CheckSessionAvailableJob",
                        CheckSessionAvailableJob.CHECK_SESSION_GROUP).build();
        Trigger session_trigger = newTrigger()
                .withIdentity("CheckSessionAvailableJob",
                        CheckSessionAvailableJob.CHECK_SESSION_GROUP)
                .withSchedule(cronSchedule(checkCronExpression)).forJob(job).build();

        scheduler = new StdSchedulerFactory("quartz.properties").getScheduler();
        // Add job to scheduler
        scheduler.scheduleJob(session_job, session_trigger);

        scheduler.start();
        ContextPreloader.ContextLog.info("Start Job Runner.");
    }

}
