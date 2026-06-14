package com.hrms.payroll.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PayableSummaryResponse {
    private int totalActiveEmployees;
    private BigDecimal totalMonthlyPayable;
    private BigDecimal totalAnnualCtc;
}
