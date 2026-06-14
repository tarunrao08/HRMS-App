package com.hrms.payroll.dto;

import com.hrms.payroll.enums.PayrollRunStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class PayrollRunResponse {

    private UUID id;
    private int year;
    private int month;
    private Instant runDate;
    private PayrollRunStatus status;
    private int totalEmployees;
    private BigDecimal totalGross;
    private BigDecimal totalDeductions;
    private BigDecimal totalNet;
    private UUID processedBy;
    private UUID approvedBy;
    private Instant approvedAt;
    private Instant processedAt;
    private UUID disbursedBy;
    private Instant disbursedAt;
    private String remarks;
    private Instant createdAt;
}
