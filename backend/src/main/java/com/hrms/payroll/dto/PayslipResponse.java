package com.hrms.payroll.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class PayslipResponse {

    private UUID id;
    private UUID payrollRunId;
    private UUID employeeId;
    private String employeeName;
    private String employeeCode;
    private int year;
    private int month;

    // Earnings (pro-rated)
    private BigDecimal basic;
    private BigDecimal hra;
    private BigDecimal da;           // stored in lta column
    private BigDecimal conveyance;   // stored in transport_allowance column
    private BigDecimal medicalAllowance;
    private BigDecimal specialAllowance;
    private BigDecimal grossSalary;

    // Deductions
    private BigDecimal tds;
    private BigDecimal otherDeductions;
    private BigDecimal totalDeductions;

    // Day counts
    private BigDecimal lopDays;
    private int workingDays;
    private BigDecimal paidDays;

    private BigDecimal netSalary;
    private String pdfUrl;
    private boolean published;
    private Instant publishedAt;
    private Instant createdAt;
}
