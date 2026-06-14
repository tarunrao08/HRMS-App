package com.hrms.onboarding.listener;

import com.hrms.onboarding.event.OnboardingCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Extension point: future modules (Payroll, Attendance) subscribe to
 * OnboardingCompletedEvent here without touching the onboarding service.
 */
@Slf4j
@Component
public class OnboardingCompletedListener {

    @EventListener
    @Async
    public void onOnboardingCompleted(OnboardingCompletedEvent event) {
        log.info("Employee {} completed onboarding (workflow: {}). " +
                 "Payroll and attendance setup can now be triggered.",
                event.getEmployeeId(), event.getWorkflowId());
    }
}
