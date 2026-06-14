package com.hrms.leave.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class LeaveBalanceResponse {

    private UUID id;
    private UUID employeeId;
    private UUID leaveTypeId;
    private String leaveTypeName;
    private String leaveTypeCode;
    private int year;
    private BigDecimal allocatedDays;
    private BigDecimal usedDays;
    private BigDecimal pendingDays;
    private BigDecimal carriedForwardDays;
    private BigDecimal lapsedDays;
    private BigDecimal availableDays;
}
