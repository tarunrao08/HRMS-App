package com.hrms.payroll.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class GeneratePayslipRequest {
    @NotNull private UUID employeeId;
    @NotNull @Min(1) @Max(12) private Integer month;
    @NotNull @Min(2000) @Max(2100) private Integer year;
}
