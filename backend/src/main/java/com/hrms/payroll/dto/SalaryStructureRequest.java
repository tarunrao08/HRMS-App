package com.hrms.payroll.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class SalaryStructureRequest {

    @NotNull
    private UUID employeeId;

    @NotNull
    @Positive
    private BigDecimal annualCtc;

    @NotNull
    private LocalDate effectiveFrom;
}
