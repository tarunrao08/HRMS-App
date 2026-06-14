package com.hrms.leave.config;

import com.hrms.leave.job.AnnualLeaveRolloverJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzLeaveConfig {

    @Value("${leave.rollover.cron:0 5 0 1 1 ?}")
    private String rolloverCron;

    @Bean
    public JobDetail annualLeaveRolloverJobDetail() {
        return JobBuilder.newJob(AnnualLeaveRolloverJob.class)
                .withIdentity("annualLeaveRolloverJob", "leave")
                .withDescription("Annual leave balance rollover — runs Jan 1 00:05")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger annualLeaveRolloverTrigger(JobDetail annualLeaveRolloverJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(annualLeaveRolloverJobDetail)
                .withIdentity("annualLeaveRolloverTrigger", "leave")
                .withSchedule(CronScheduleBuilder.cronSchedule(rolloverCron))
                .build();
    }
}
