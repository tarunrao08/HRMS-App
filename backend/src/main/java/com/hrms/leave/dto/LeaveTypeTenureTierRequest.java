package com.hrms.leave.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LeaveTypeTenureTierRequest {

    @NotNull(message = "Minimum years is required")
    @Min(value = 0, message = "Minimum years cannot be negative")
    private Integer minYears;

    /** Exclusive upper bound; null means unbounded (e.g. "5+ years"). */
    private Integer maxYears;

    @NotNull(message = "Days is required")
    @Min(value = 0, message = "Days cannot be negative")
    private Integer days;
}
