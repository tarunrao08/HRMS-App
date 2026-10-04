package com.hrms.leave.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class LeaveBalanceAdjustmentResponse {
    private UUID id;
    private UUID leaveBalanceId;
    private BigDecimal days;
    private String reason;
    private String adjustedByName;
    private Instant createdAt;
}
