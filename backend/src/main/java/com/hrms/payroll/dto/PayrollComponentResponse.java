package com.hrms.payroll.dto;

import com.hrms.payroll.enums.CalculationType;
import com.hrms.payroll.enums.ComponentType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class PayrollComponentResponse {

    private UUID id;
    private String name;
    private String code;
    private ComponentType componentType;
    private CalculationType calculationType;
    private BigDecimal value;
    private String percentageOf;
    private boolean taxable;
    private boolean active;
    private int displayOrder;
    private Instant createdAt;
}
