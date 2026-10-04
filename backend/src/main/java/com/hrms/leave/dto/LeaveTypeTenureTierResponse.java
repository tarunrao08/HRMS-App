package com.hrms.leave.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class LeaveTypeTenureTierResponse {
    private UUID id;
    private UUID leaveTypeId;
    private int minYears;
    private Integer maxYears;
    private int days;
}
