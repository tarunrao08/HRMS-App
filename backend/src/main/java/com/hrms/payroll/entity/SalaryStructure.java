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

    // Earning breakdown (Basic, HRA, DA, ...) lives in SalaryStructureComponent rows —
    // an open-ended, catalog-driven list instead of fixed columns here.

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
