package com.hrms.payroll.dto;

import com.hrms.payroll.enums.TaxRegime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class TdsDeclarationRequest {

    @NotNull
    private UUID employeeId;

    @NotBlank
    private String financialYear;

    @NotNull
    private TaxRegime regimeType;

    private BigDecimal hraExemption = BigDecimal.ZERO;

    private BigDecimal section80c = BigDecimal.ZERO;

    private BigDecimal section80d = BigDecimal.ZERO;

    private BigDecimal section80g = BigDecimal.ZERO;

    private BigDecimal otherDeductions = BigDecimal.ZERO;
}
