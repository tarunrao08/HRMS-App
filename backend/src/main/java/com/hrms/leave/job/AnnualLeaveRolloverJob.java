package com.hrms.leave.job;

import com.hrms.leave.service.LeaveRolloverService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

/**
 * Quartz job that fires on Jan 1 at 00:05 (configurable via leave.rollover.cron) and runs
 * the annual leave rollover — allocating new-year balances and carrying forward eligible days.
 * Can also be triggered manually via POST /api/leave/admin/run-annual-rollover?year=YYYY.
 */
@Slf4j
@DisallowConcurrentExecution
public class AnnualLeaveRolloverJob implements Job {

    // Field injection — Quartz creates instances via newInstance(); Spring's SpringBeanJobFactory
    // then processes @Autowired fields automatically.
    @Autowired
    private LeaveRolloverService leaveRolloverService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getMergedJobDataMap();
        int year = dataMap.containsKey("year")
                ? dataMap.getInt("year")
                : LocalDate.now().getYear();

        log.info("AnnualLeaveRolloverJob triggered for year {}", year);
        try {
            int count = leaveRolloverService.runRollover(year);
            log.info("AnnualLeaveRolloverJob completed for year {} — {} employees processed", year, count);
        } catch (Exception e) {
            log.error("AnnualLeaveRolloverJob failed for year {}", year, e);
            throw new JobExecutionException(e);
        }
    }
}
