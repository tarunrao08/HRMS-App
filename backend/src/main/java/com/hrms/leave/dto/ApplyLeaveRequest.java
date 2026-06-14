package com.hrms.leave.dto;

import com.hrms.leave.enums.HalfDayType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class ApplyLeaveRequest {

    @NotNull
    private UUID leaveTypeId;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    private boolean halfDay;

    private HalfDayType halfDayType;

    @NotBlank
    private String reason;

    private String documentUrl;
}
