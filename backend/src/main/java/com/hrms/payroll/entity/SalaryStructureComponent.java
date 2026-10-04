package com.hrms.payroll.entity;

import com.hrms.common.entity.BaseEntity;
import com.hrms.payroll.enums.CalculationType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * One earning-component line on an employee's salary structure — the per-employee,
 * editable percentage/fixed-amount override for a PayrollComponent catalog entry.
 * calculationType/value are snapshotted per line (independent of later catalog edits);
 * computedAmount is the resolved monthly ₹ amount at the time this structure was built.
 */
@Entity
@Table(name = "salary_structure_components")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryStructureComponent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salary_structure_id", nullable = false)
    private SalaryStructure salaryStructure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_component_id", nullable = false)
    private PayrollComponent payrollComponent;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_type", nullable = false, length = 20)
    private CalculationType calculationType;

    @Column(name = "value", precision = 12, scale = 4)
    private BigDecimal value;

    @Column(name = "computed_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal computedAmount;
}
