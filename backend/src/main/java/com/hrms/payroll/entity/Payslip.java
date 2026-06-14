package com.hrms.payroll.entity;

import com.hrms.common.entity.BaseEntity;
import com.hrms.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payslips", uniqueConstraints = @UniqueConstraint(columnNames = {"payroll_run_id", "employee_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payslip extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_run_id", nullable = false)
    private PayrollRun payrollRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "month", nullable = false)
    private int month;

    @Column(name = "basic", precision = 14, scale = 2)
    private BigDecimal basic;

    @Column(name = "hra", precision = 14, scale = 2)
    private BigDecimal hra;

    @Column(name = "special_allowance", precision = 14, scale = 2)
    private BigDecimal specialAllowance;

    @Column(name = "medical_allowance", precision = 14, scale = 2)
    private BigDecimal medicalAllowance;

    @Column(name = "conveyance", precision = 14, scale = 2)
    private BigDecimal conveyance;

    @Column(name = "da", precision = 14, scale = 2)
    private BigDecimal da;

    @Column(name = "other_earnings", precision = 14, scale = 2)
    private BigDecimal otherEarnings;

    @Column(name = "gross_salary", precision = 14, scale = 2)
    private BigDecimal grossSalary;

    @Column(name = "pf_deduction", precision = 14, scale = 2)
    private BigDecimal pfDeduction;

    @Column(name = "esi_deduction", precision = 14, scale = 2)
    private BigDecimal esiDeduction;

    @Column(name = "professional_tax", precision = 14, scale = 2)
    private BigDecimal professionalTax;

    @Column(name = "tds", precision = 14, scale = 2)
    private BigDecimal tds;

    @Column(name = "loan_deduction", precision = 14, scale = 2)
    private BigDecimal loanDeduction;

    @Column(name = "advance_deduction", precision = 14, scale = 2)
    private BigDecimal advanceDeduction;

    @Column(name = "other_deductions", precision = 14, scale = 2)
    private BigDecimal otherDeductions;

    @Column(name = "total_deductions", precision = 14, scale = 2)
    private BigDecimal totalDeductions;

    @Column(name = "lop_days", precision = 5, scale = 2)
    private BigDecimal lopDays;

    @Column(name = "lop_amount", precision = 14, scale = 2)
    private BigDecimal lopAmount;

    @Column(name = "net_salary", precision = 14, scale = 2)
    private BigDecimal netSalary;

    @Column(name = "working_days")
    private int workingDays;

    @Column(name = "paid_days", precision = 5, scale = 2)
    private BigDecimal paidDays;

    @Column(name = "pdf_url")
    private String pdfUrl;

    @Column(name = "is_published")
    private boolean published;

    @Column(name = "published_at")
    private Instant publishedAt;
}
