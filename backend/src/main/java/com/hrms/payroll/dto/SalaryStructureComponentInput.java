package com.hrms.payroll.dto;

import com.hrms.payroll.enums.CalculationType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A per-employee override for one earning component on a salary structure request.
 * Any active earning component not listed here falls back to its catalog default
 * (calculationType/value from PayrollComponent) — HR only needs to specify what
 * they're overriding for this employee (e.g. just Basic and HRA).
 */
@Getter
@Setter
public class SalaryStructureComponentInput {

    @NotNull
    private UUID payrollComponentId;

    /** Defaults to the catalog component's own calculationType if omitted. */
    private CalculationType calculationType;

    /** Defaults to the catalog component's own value if omitted. Ignored (must be null) for FORMULA. */
    private BigDecimal value;
}
