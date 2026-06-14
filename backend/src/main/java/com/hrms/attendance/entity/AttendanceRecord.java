package com.hrms.attendance.entity;

import com.hrms.attendance.enums.AttendanceStatus;
import com.hrms.common.entity.BaseEntity;
import com.hrms.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "attendance_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "punch_in")
    private Instant punchIn;

    @Column(name = "punch_out")
    private Instant punchOut;

    @Column(name = "working_hours", precision = 5, scale = 2)
    private BigDecimal workingHours;

    @Column(name = "overtime_hours", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal overtimeHours = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private AttendanceStatus status = AttendanceStatus.ABSENT;

    @Column(name = "punch_in_location", length = 200)
    private String punchInLocation;

    @Column(name = "punch_out_location", length = 200)
    private String punchOutLocation;

    @Column(name = "punch_in_ip", length = 45)
    private String punchInIp;

    @Column(name = "punch_out_ip", length = 45)
    private String punchOutIp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    private Shift shift;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "is_regularized")
    @Builder.Default
    private boolean regularized = false;

    @Column(name = "regularization_reason", columnDefinition = "TEXT")
    private String regularizationReason;

    @Column(name = "regularized_by")
    private UUID regularizedBy;

    @Column(name = "regularized_at")
    private Instant regularizedAt;
}
