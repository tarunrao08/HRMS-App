package com.hrms.attendance.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Builder
public class ShiftResponse {

    private UUID id;
    private String name;
    private LocalTime startTime;
    private LocalTime endTime;
    private int gracePeriodMinutes;
    private BigDecimal workingHours;
    private boolean nightShift;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
