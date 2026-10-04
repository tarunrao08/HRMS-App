package com.hrms.payroll.entity;

import com.hrms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** One earning-component line on a generated payslip — the pro-rated amount for that period. */
@Entity
@Table(name = "payslip_components")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayslipComponent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payslip_id", nullable = false)
    private Payslip payslip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_component_id", nullable = false)
    private PayrollComponent payrollComponent;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;
}
