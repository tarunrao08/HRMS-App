package com.hrms.attendance.job;

import com.hrms.attendance.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Fires once per shift's grace-period cutoff (start time + grace minutes) instead of
 * polling on a fixed interval, since the shifts are fixed and known ahead of time.
 * Each firing runs the same check across all employees; anyone whose own shift cutoff
 * hasn't passed yet is simply skipped until their shift's own cron fires.
 * Cron times must be updated here if a shift's start time or grace period changes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoMarkAbsentScheduler {

    private final AttendanceService attendanceService;

    @Scheduled(cron = "${attendance.auto-absent.cron.general-shift:0 30 9 * * MON-FRI}")   // General Shift   09:00 + 30 min grace
    @Scheduled(cron = "${attendance.auto-absent.cron.afternoon-shift:0 30 14 * * MON-FRI}") // Afternoon Shift 14:00 + 30 min grace
    @Scheduled(cron = "${attendance.auto-absent.cron.night-shift:0 30 22 * * MON-FRI}")     // Night Shift     22:00 + 30 min grace
    public void runAutoAbsentCheck() {
        try {
            int count = attendanceService.runAutoAbsentCheck();
            if (count > 0) {
                log.info("Auto-absent check marked {} employee(s) absent", count);
            }
        } catch (Exception e) {
            log.error("Auto-absent check failed", e);
        }
    }
}
