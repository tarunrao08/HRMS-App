package com.hrms.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class AdjustLeaveBalanceRequest {

    @NotNull(message = "Leave type is required")
    private UUID leaveTypeId;

    @NotNull(message = "Year is required")
    private Integer year;

    @NotNull(message = "Days is required")
    private BigDecimal days;

    @NotBlank(message = "Reason is required")
    private String reason;
}
