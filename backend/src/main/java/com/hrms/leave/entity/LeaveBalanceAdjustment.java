package com.hrms.leave.entity;

import com.hrms.common.entity.BaseEntity;
import com.hrms.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "leave_balance_adjustments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveBalanceAdjustment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_balance_id", nullable = false)
    private LeaveBalance leaveBalance;

    @Column(name = "days", nullable = false, precision = 5, scale = 2)
    private BigDecimal days;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "adjusted_by", nullable = false)
    private Employee adjustedBy;
}
