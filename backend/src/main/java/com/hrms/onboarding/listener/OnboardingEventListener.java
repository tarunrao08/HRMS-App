package com.hrms.onboarding.listener;

import com.hrms.employee.event.EmployeeCreatedEvent;
import com.hrms.onboarding.service.OnboardingWorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OnboardingEventListener {

    private final OnboardingWorkflowService workflowService;

    /**
     * Fires after the employee row is committed so the workflow creation can
     * safely reference it. Runs asynchronously to not block the HTTP response.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onEmployeeCreated(EmployeeCreatedEvent event) {
        try {
            workflowService.initiateOnboarding(event.getEmployeeId());
            log.info("Auto-initiated onboarding for employee {}", event.getEmployeeId());
        } catch (Exception ex) {
            log.error("Failed to auto-initiate onboarding for employee {}: {}",
                    event.getEmployeeId(), ex.getMessage(), ex);
        }
    }
}
