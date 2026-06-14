package com.hrms.leave.entity;

import com.hrms.common.entity.BaseEntity;
import com.hrms.employee.entity.Employee;
import com.hrms.leave.enums.HalfDayType;
import com.hrms.leave.enums.LeaveRequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "leave_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_days", nullable = false, precision = 5, scale = 2)
    private BigDecimal totalDays;

    @Column(name = "half_day")
    @Builder.Default
    private boolean halfDay = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "half_day_type", length = 10)
    private HalfDayType halfDayType;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private LeaveRequestStatus status = LeaveRequestStatus.PENDING;

    @Column(name = "applied_at")
    @Builder.Default
    private Instant appliedAt = Instant.now();

    @Column(name = "document_url")
    private String documentUrl;

    @Column(name = "current_approval_level")
    @Builder.Default
    private int currentApprovalLevel = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_approver_id")
    private Employee currentApprover;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    // Stored as plain UUID to match the schema FK referencing users(id) without a User entity mapped here
    @Column(name = "cancelled_by")
    private UUID cancelledBy;
}
