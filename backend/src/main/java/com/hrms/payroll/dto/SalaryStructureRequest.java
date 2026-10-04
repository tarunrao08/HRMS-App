package com.hrms.payroll.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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

    /**
     * Per-employee overrides for earning components (e.g. this employee's Basic %, HRA %).
     * Any active earning component not listed here uses its catalog default. Null/empty
     * means "use the catalog as-is for every active earning component."
     */
    @Valid
    private List<SalaryStructureComponentInput> components;
}
