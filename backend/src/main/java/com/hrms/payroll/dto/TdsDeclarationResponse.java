package com.hrms.payroll.dto;

import com.hrms.payroll.enums.TaxRegime;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class TdsDeclarationResponse {

    private UUID id;
    private UUID employeeId;
    private String employeeName;
    private String financialYear;
    private TaxRegime regimeType;
    private BigDecimal hraExemption;
    private BigDecimal section80c;
    private BigDecimal section80d;
    private BigDecimal section80g;
    private BigDecimal otherDeductions;
    private Instant declaredAt;
    private boolean verified;
    private Instant createdAt;
}
