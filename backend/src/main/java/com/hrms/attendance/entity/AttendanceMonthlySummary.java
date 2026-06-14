package com.hrms.attendance.entity;

import com.hrms.common.entity.BaseEntity;
import com.hrms.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "attendance_monthly_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceMonthlySummary extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "month", nullable = false)
    private int month;

    @Column(name = "present_days")
    @Builder.Default
    private int presentDays = 0;

    @Column(name = "absent_days")
    @Builder.Default
    private int absentDays = 0;

    @Column(name = "late_days")
    @Builder.Default
    private int lateDays = 0;

    @Column(name = "half_days")
    @Builder.Default
    private int halfDays = 0;

    @Column(name = "overtime_hours", precision = 7, scale = 2)
    @Builder.Default
    private BigDecimal overtimeHours = BigDecimal.ZERO;

    @Column(name = "total_working_hours", precision = 7, scale = 2)
    @Builder.Default
    private BigDecimal totalWorkingHours = BigDecimal.ZERO;

    @Column(name = "working_days")
    @Builder.Default
    private int workingDays = 0;
}
