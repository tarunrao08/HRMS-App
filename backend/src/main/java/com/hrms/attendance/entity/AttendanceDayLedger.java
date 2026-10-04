package com.hrms.attendance.entity;

import com.hrms.attendance.enums.DaySegmentCategory;
import com.hrms.common.entity.BaseEntity;
import com.hrms.employee.entity.Employee;
import com.hrms.leave.entity.LeaveType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Canonical per-day, per-half (AM/PM) attendance-and-leave classification for one employee.
 * This is payroll's single source of truth for paid-day / loss-of-pay computation —
 * it is reconciled from AttendanceRecord (presence facts) and LeaveRequest (approved leave)
 * whenever either changes, so payroll never has to join those tables itself.
 */
@Entity
@Table(name = "attendance_day_ledger", uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "ledger_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceDayLedger extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "ledger_date", nullable = false)
    private LocalDate ledgerDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "am_category", nullable = false, length = 20)
    private DaySegmentCategory amCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "am_leave_type_id")
    private LeaveType amLeaveType;

    @Enumerated(EnumType.STRING)
    @Column(name = "pm_category", nullable = false, length = 20)
    private DaySegmentCategory pmCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pm_leave_type_id")
    private LeaveType pmLeaveType;
}
