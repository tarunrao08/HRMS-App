package com.hrms.leave.dto;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class LeaveTypeResponse {

    private UUID id;
    private String name;
    private String code;
    private String description;
    private int maxDaysPerYear;
    private boolean carryForwardAllowed;
    private int maxCarryForwardDays;
    private boolean encashmentAllowed;
    private boolean paid;
    private boolean requiresDocument;
    private int minNoticeDays;
    private boolean active;
    private Instant createdAt;
}
