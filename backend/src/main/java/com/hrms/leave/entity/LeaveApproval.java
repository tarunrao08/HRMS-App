package com.hrms.leave.entity;

import com.hrms.common.entity.BaseEntity;
import com.hrms.employee.entity.Employee;
import com.hrms.leave.enums.LeaveApprovalAction;
import com.hrms.leave.enums.LeaveApprovalStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "leave_approvals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveApproval extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_request_id", nullable = false)
    private LeaveRequest leaveRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approver_id", nullable = false)
    private Employee approver;

    @Column(name = "approver_level")
    @Builder.Default
    private int approverLevel = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private LeaveApprovalStatus status = LeaveApprovalStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 20)
    private LeaveApprovalAction action;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "acted_at")
    private Instant actedAt;
}
