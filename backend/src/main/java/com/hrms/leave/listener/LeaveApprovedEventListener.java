package com.hrms.leave.listener;

import com.hrms.leave.event.LeaveApprovedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class LeaveApprovedEventListener {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLeaveApproved(LeaveApprovedEvent event) {
        log.info("Leave approved for employee {}, dates {} to {}",
                event.getEmployeeId(), event.getStartDate(), event.getEndDate());
    }
}
