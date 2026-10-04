package com.hrms.attendance.service.impl;

import com.hrms.attendance.entity.AttendanceDayLedger;
import com.hrms.attendance.entity.AttendanceRecord;
import com.hrms.attendance.enums.AttendanceStatus;
import com.hrms.attendance.enums.DaySegmentCategory;
import com.hrms.attendance.repository.AttendanceDayLedgerRepository;
import com.hrms.attendance.repository.AttendanceRecordRepository;
import com.hrms.attendance.service.AttendanceDayLedgerService;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.leave.entity.LeaveRequest;
import com.hrms.leave.entity.LeaveType;
import com.hrms.leave.enums.HalfDayType;
import com.hrms.leave.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceDayLedgerServiceImpl implements AttendanceDayLedgerService {

    private static final BigDecimal HALF_DAY = new BigDecimal("0.5");

    // Attendance statuses that represent an actual worked (or WFH/regularized-as-worked) day
    private static final Set<AttendanceStatus> WORKED_STATUSES = Set.of(
            AttendanceStatus.PRESENT, AttendanceStatus.LATE,
            AttendanceStatus.WORK_FROM_HOME, AttendanceStatus.REGULARIZED);

    private final AttendanceDayLedgerRepository ledgerRepository;
    private final AttendanceRecordRepository    attendanceRecordRepository;
    private final LeaveRequestRepository        leaveRequestRepository;
    private final EmployeeRepository            employeeRepository;

    @Override
    public AttendanceDayLedger reconcileDate(UUID employeeId, LocalDate date) {
        return reconcileRange(employeeId, date, date).get(0);
    }

    @Override
    public List<AttendanceDayLedger> reconcileRange(UUID employeeId, LocalDate from, LocalDate to) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId.toString()));

        Map<LocalDate, AttendanceRecord> recordsByDate = attendanceRecordRepository
                .findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(employeeId, from, to)
                .stream()
                .collect(Collectors.toMap(AttendanceRecord::getAttendanceDate, r -> r, (a, b) -> a));

        List<LeaveRequest> approvedLeaves = leaveRequestRepository
                .findApprovedOverlapping(employeeId, from, to);

        Map<LocalDate, AttendanceDayLedger> existingLedgers = ledgerRepository
                .findByEmployeeIdAndLedgerDateBetween(employeeId, from, to)
                .stream()
                .collect(Collectors.toMap(AttendanceDayLedger::getLedgerDate, l -> l, (a, b) -> a));

        List<AttendanceDayLedger> result = new java.util.ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            final LocalDate currentDate = date;
            List<LeaveRequest> leavesForDate = approvedLeaves.stream()
                    .filter(lr -> !lr.getStartDate().isAfter(currentDate) && !lr.getEndDate().isBefore(currentDate))
                    .toList();

            DaySegments segments = classify(date, recordsByDate.get(date), leavesForDate);

            AttendanceDayLedger ledger = existingLedgers.get(date);
            if (ledger == null) {
                ledger = AttendanceDayLedger.builder().employee(employee).ledgerDate(date).build();
            }
            ledger.setAmCategory(segments.am().category());
            ledger.setAmLeaveType(segments.am().leaveType());
            ledger.setPmCategory(segments.pm().category());
            ledger.setPmLeaveType(segments.pm().leaveType());
            result.add(ledgerRepository.save(ledger));
        }
        return result;
    }

    @Override
    public BigDecimal getLopDays(UUID employeeId, LocalDate from, LocalDate to) {
        BigDecimal lop = BigDecimal.ZERO;
        for (AttendanceDayLedger ledger : reconcileRange(employeeId, from, to)) {
            lop = lop.add(segmentLopFraction(ledger.getAmCategory(), ledger.getAmLeaveType()));
            lop = lop.add(segmentLopFraction(ledger.getPmCategory(), ledger.getPmLeaveType()));
        }
        return lop;
    }

    // ── Classification ──────────────────────────────────────────────────────

    private DaySegments classify(LocalDate date, AttendanceRecord record, List<LeaveRequest> leavesForDate) {
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            Segment weekend = new Segment(DaySegmentCategory.WEEKEND, null);
            return new DaySegments(weekend, weekend);
        }

        Segment am = null;
        Segment pm = null;
        for (LeaveRequest lr : leavesForDate) {
            if (!lr.isHalfDay()) {
                am = new Segment(DaySegmentCategory.LEAVE, lr.getLeaveType());
                pm = new Segment(DaySegmentCategory.LEAVE, lr.getLeaveType());
            } else if (lr.getHalfDayType() == HalfDayType.FIRST_HALF) {
                am = new Segment(DaySegmentCategory.LEAVE, lr.getLeaveType());
            } else if (lr.getHalfDayType() == HalfDayType.SECOND_HALF) {
                pm = new Segment(DaySegmentCategory.LEAVE, lr.getLeaveType());
            }
        }

        // A manual HALF_DAY regularization (no associated leave) can't say which half was
        // worked, so split it evenly rather than collapsing the whole day to one bucket.
        if (am == null && pm == null && record != null && record.getStatus() == AttendanceStatus.HALF_DAY) {
            return new DaySegments(new Segment(DaySegmentCategory.WORKED, null), new Segment(DaySegmentCategory.ABSENT, null));
        }

        Segment derived = deriveFromAttendance(date, record);
        if (am == null) am = derived;
        if (pm == null) pm = derived;
        return new DaySegments(am, pm);
    }

    /**
     * Approximates a half's status from the whole-day AttendanceRecord, since punches
     * aren't currently tracked per half-day. A half not covered by approved leave is
     * assumed WORKED whenever the day shows any worked status; an explicit ABSENT, or no
     * record at all for a day that has already elapsed, is treated as ABSENT (LOP).
     * A day that hasn't concluded yet (today/future with no record) is left UNMARKED
     * rather than penalized.
     */
    private Segment deriveFromAttendance(LocalDate date, AttendanceRecord record) {
        if (record != null) {
            AttendanceStatus status = record.getStatus();
            if (status == AttendanceStatus.HOLIDAY) return new Segment(DaySegmentCategory.HOLIDAY, null);
            if (status == AttendanceStatus.WEEKEND) return new Segment(DaySegmentCategory.WEEKEND, null);
            if (WORKED_STATUSES.contains(status)) return new Segment(DaySegmentCategory.WORKED, null);
            return new Segment(DaySegmentCategory.ABSENT, null);
        }
        return date.isBefore(LocalDate.now())
                ? new Segment(DaySegmentCategory.ABSENT, null)
                : new Segment(DaySegmentCategory.UNMARKED, null);
    }

    private BigDecimal segmentLopFraction(DaySegmentCategory category, LeaveType leaveType) {
        boolean isLop = switch (category) {
            case ABSENT -> true;
            case LEAVE -> leaveType != null && !leaveType.isPaid();
            case WORKED, WEEKEND, HOLIDAY, UNMARKED -> false;
        };
        return isLop ? HALF_DAY : BigDecimal.ZERO;
    }

    private record Segment(DaySegmentCategory category, LeaveType leaveType) {}

    private record DaySegments(Segment am, Segment pm) {}
}
