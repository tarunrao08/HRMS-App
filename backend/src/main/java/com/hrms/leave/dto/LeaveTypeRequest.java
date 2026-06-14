package com.hrms.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LeaveTypeRequest {

    @NotBlank
    private String name;

    @NotBlank
    @Size(max = 10)
    private String code;

    private String description;

    private int maxDaysPerYear;

    private boolean carryForwardAllowed;

    private int maxCarryForwardDays;

    private boolean encashmentAllowed;

    private boolean paid;

    private boolean requiresDocument;

    private int minNoticeDays;

    private boolean active = true;
}
