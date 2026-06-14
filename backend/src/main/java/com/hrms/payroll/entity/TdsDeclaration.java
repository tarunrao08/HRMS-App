package com.hrms.payroll.entity;

import com.hrms.common.entity.BaseEntity;
import com.hrms.employee.entity.Employee;
import com.hrms.payroll.enums.TaxRegime;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tds_declarations", uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "financial_year"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TdsDeclaration extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "financial_year", nullable = false, length = 9)
    private String financialYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "regime_type", length = 10)
    private TaxRegime regimeType;

    @Column(name = "hra_exemption", precision = 14, scale = 2)
    private BigDecimal hraExemption;

    @Column(name = "section_80c", precision = 14, scale = 2)
    private BigDecimal section80c;

    @Column(name = "section_80d", precision = 14, scale = 2)
    private BigDecimal section80d;

    @Column(name = "section_80g", precision = 14, scale = 2)
    private BigDecimal section80g;

    @Column(name = "other_deductions", precision = 14, scale = 2)
    private BigDecimal otherDeductions;

    @Column(name = "declared_at")
    private Instant declaredAt;

    @Column(name = "is_verified")
    private boolean verified;
}
