package com.hrms.leave.listener;

import com.hrms.leave.service.AllocationService;
import com.hrms.onboarding.event.OnboardingCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;

/**
 * Triggers leave balance allocation when an employee's onboarding workflow completes.
 * Runs async after the onboarding transaction commits so allocation has no impact on the onboarding flow.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LeaveAllocationOnboardingListener {

    private final AllocationService allocationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOnboardingCompleted(OnboardingCompletedEvent event) {
        int currentYear = LocalDate.now().getYear();
        log.info("Onboarding completed for employee {} — allocating leave balances for year {}",
                event.getEmployeeId(), currentYear);
        try {
            allocationService.allocateLeaveForEmployee(event.getEmployeeId(), currentYear);
            log.info("Leave balances allocated for employee {} year {}", event.getEmployeeId(), currentYear);
        } catch (Exception e) {
            log.error("Failed to allocate leave balances for employee {} after onboarding: {}",
                    event.getEmployeeId(), e.getMessage(), e);
        }
    }
}
