package com.hrms.payroll.dto;

import com.hrms.payroll.enums.CalculationType;
import com.hrms.payroll.enums.ComponentType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class SalaryStructureComponentResponse {
    private UUID payrollComponentId;
    private String code;
    private String name;
    private ComponentType componentType;
    private CalculationType calculationType;
    private BigDecimal value;
    private BigDecimal computedAmount;
    private int displayOrder;
}
