package com.hrms.payroll.dto;

import com.hrms.payroll.enums.CalculationType;
import com.hrms.payroll.enums.ComponentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class PayrollComponentRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String code;

    @NotNull
    private ComponentType componentType;

    @NotNull
    private CalculationType calculationType;

    private BigDecimal value;

    /** Component this percentage is taken of, when calculationType is PERCENTAGE. Null = percentage of monthly gross. */
    private UUID percentageOfComponentId;

    private boolean taxable;

    private boolean active = true;

    private int displayOrder;
}
