package com.hesong.weChatAdapter.context;

import java.io.IOException;

import static org.quartz.TriggerBuilder.*;
import static org.quartz.CronScheduleBuilder.*;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

/**
 * 抓取邮件流程触发器
 * 
 * @author Bowen
 * 
 */
public class UpdateAccessTokenRunner {

    public static Scheduler scheduler;

    public void task() throws IOException, SchedulerException {

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

        scheduler.start();
        ContextPreloader.ContextLog.info("Start UpdateAccessTokenRunner.");
    }

}
