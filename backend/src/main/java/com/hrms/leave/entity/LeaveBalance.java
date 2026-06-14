package com.hrms.leave.entity;

import com.hrms.common.entity.BaseEntity;
import com.hrms.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
    name = "leave_balances",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_leave_balance_employee_type_year",
        columnNames = {"employee_id", "leave_type_id", "year"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveBalance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "allocated_days", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal allocatedDays = BigDecimal.ZERO;

    @Column(name = "used_days", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal usedDays = BigDecimal.ZERO;

    @Column(name = "pending_days", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal pendingDays = BigDecimal.ZERO;

    @Column(name = "carried_forward_days", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal carriedForwardDays = BigDecimal.ZERO;

    @Column(name = "lapsed_days", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal lapsedDays = BigDecimal.ZERO;
}
