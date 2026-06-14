package com.hrms.payroll.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InitiatePayrollRunRequest {

    @NotNull
    @Min(2000)
    @Max(2099)
    private Integer year;

    @NotNull
    @Min(1)
    @Max(12)
    private Integer month;

    private String remarks;
}
