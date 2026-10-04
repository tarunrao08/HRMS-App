package com.hrms.payroll.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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

    // Earning component breakdown (pro-rated) — open-ended, catalog-driven
    private List<PayslipComponentResponse> components;
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
