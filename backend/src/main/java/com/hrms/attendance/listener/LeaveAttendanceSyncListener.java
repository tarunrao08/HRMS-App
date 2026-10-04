package com.hrms.attendance.listener;

import com.hrms.attendance.entity.AttendanceRecord;
import com.hrms.attendance.enums.AttendanceStatus;
import com.hrms.attendance.repository.AttendanceRecordRepository;
import com.hrms.attendance.service.AttendanceDayLedgerService;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.leave.event.LeaveApprovedEvent;
import com.hrms.leave.event.LeaveCancelledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Keeps attendance in sync with approved/cancelled leave. Full-day leave locks the
 * attendance page to ON_LEAVE for that day (unchanged). Half-day leave deliberately
 * does NOT touch AttendanceRecord — punchIn() refuses to punch in on a date that
 * already has a record, so writing anything there would make it impossible to punch
 * in for the working half. Instead, the attendance day ledger (single source of truth
 * for payroll) combines the approved half-day leave — authoritative for its own half —
 * with whatever attendance ends up recorded for the other half.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LeaveAttendanceSyncListener {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceDayLedgerService attendanceDayLedgerService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLeaveApproved(LeaveApprovedEvent event) {
        if (!event.isHalfDay()) {
            Optional<Employee> employee = employeeRepository.findById(event.getEmployeeId());
            if (employee.isEmpty()) {
                log.warn("LeaveApprovedEvent for unknown employee {}", event.getEmployeeId());
                return;
            }
            forEachWeekday(event.getStartDate(), event.getEndDate(), date -> {
                Optional<AttendanceRecord> existing =
                        attendanceRecordRepository.findByEmployeeIdAndAttendanceDate(event.getEmployeeId(), date);
                if (existing.isPresent()) {
                    AttendanceRecord record = existing.get();
                    boolean backedByRealData = record.getStatus() != AttendanceStatus.ABSENT
                            || record.getPunchIn() != null
                            || record.getPunchOut() != null
                            || record.isRegularized();
                    if (backedByRealData) {
                        // Never overwrite a record backed by a punch, manual PRESENT mark, or prior
                        // regularization. A bare ABSENT record (auto- or manually-marked, no punches)
                        // has no real data to protect, so approved leave supersedes it.
                        return;
                    }
                    record.setStatus(AttendanceStatus.ON_LEAVE);
                    attendanceRecordRepository.save(record);
                    return;
                }
                AttendanceRecord record = AttendanceRecord.builder()
                        .employee(employee.get())
                        .attendanceDate(date)
                        .status(AttendanceStatus.ON_LEAVE)
                        .build();
                attendanceRecordRepository.save(record);
            });
        }
        attendanceDayLedgerService.reconcileRange(event.getEmployeeId(), event.getStartDate(), event.getEndDate());
        log.info("Synced attendance for employee {} from {} to {} (halfDay={})",
                event.getEmployeeId(), event.getStartDate(), event.getEndDate(), event.isHalfDay());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLeaveCancelled(LeaveCancelledEvent event) {
        forEachWeekday(event.getStartDate(), event.getEndDate(), date ->
                attendanceRecordRepository.findByEmployeeIdAndAttendanceDate(event.getEmployeeId(), date)
                        .filter(r -> r.getStatus() == AttendanceStatus.ON_LEAVE)
                        .ifPresent(attendanceRecordRepository::delete));
        attendanceDayLedgerService.reconcileRange(event.getEmployeeId(), event.getStartDate(), event.getEndDate());
        log.info("Reverted auto-marked attendance for employee {} from {} to {}",
                event.getEmployeeId(), event.getStartDate(), event.getEndDate());
    }

    private void forEachWeekday(LocalDate start, LocalDate end, java.util.function.Consumer<LocalDate> action) {
        LocalDate d = start;
        while (!d.isAfter(end)) {
            DayOfWeek dow = d.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                action.accept(d);
            }
            d = d.plusDays(1);
        }
    }
}
