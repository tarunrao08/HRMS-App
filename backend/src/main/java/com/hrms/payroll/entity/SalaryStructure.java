package com.hrms.payroll.entity;

import com.hrms.common.entity.BaseEntity;
import com.hrms.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "salary_structures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryStructure extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "annual_ctc", nullable = false, precision = 14, scale = 2)
    private BigDecimal annualCtc;

    @Column(name = "ctc", nullable = false, precision = 14, scale = 2)
    private BigDecimal ctc;

    @Column(name = "basic", nullable = false, precision = 14, scale = 2)
    private BigDecimal basic;

    @Column(name = "hra", precision = 14, scale = 2)
    private BigDecimal hra;

    @Column(name = "da", precision = 14, scale = 2)
    private BigDecimal da;

    @Column(name = "conveyance", precision = 14, scale = 2)
    private BigDecimal conveyance;

    @Column(name = "special_allowance", precision = 14, scale = 2)
    private BigDecimal specialAllowance;

    @Column(name = "medical_allowance", precision = 14, scale = 2)
    private BigDecimal medicalAllowance;

    @Column(name = "transport_allowance", precision = 14, scale = 2)
    private BigDecimal transportAllowance;

    @Column(name = "lta", precision = 14, scale = 2)
    private BigDecimal lta;

    @Column(name = "pf_applicable")
    private boolean pfApplicable;

    @Column(name = "esi_applicable")
    private boolean esiApplicable;

    @Column(name = "gross_salary", nullable = false, precision = 14, scale = 2)
    private BigDecimal grossSalary;

    @Column(name = "pf_employee", precision = 14, scale = 2)
    private BigDecimal pfEmployee;

    @Column(name = "pf_employer", precision = 14, scale = 2)
    private BigDecimal pfEmployer;

    @Column(name = "esi_employee", precision = 14, scale = 2)
    private BigDecimal esiEmployee;

    @Column(name = "esi_employer", precision = 14, scale = 2)
    private BigDecimal esiEmployer;

    @Column(name = "professional_tax", precision = 14, scale = 2)
    private BigDecimal professionalTax;

    @Column(name = "net_salary", nullable = false, precision = 14, scale = 2)
    private BigDecimal netSalary;

    @Column(name = "is_active")
    private boolean active;
}
