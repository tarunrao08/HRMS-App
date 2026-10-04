package com.hrms.payroll.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class SalaryStructureResponse {

    private UUID id;
    private UUID employeeId;
    private String employeeName;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;

    private BigDecimal annualCtc;
    private BigDecimal monthlyGross;

    // Earning component breakdown — open-ended, catalog-driven (see SalaryStructureComponent)
    private List<SalaryStructureComponentResponse> components;

    // Statutory deductions (pre-computed at structure creation time)
    private BigDecimal pfEmployee;
    private BigDecimal esiEmployee;
    private BigDecimal professionalTax;
    private BigDecimal netSalary;

    private boolean pfApplicable;
    private boolean esiApplicable;
    private boolean active;
    private Instant createdAt;
}
