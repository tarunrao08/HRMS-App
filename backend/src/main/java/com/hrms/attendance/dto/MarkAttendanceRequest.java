package com.hrms.attendance.dto;

import com.hrms.attendance.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.UUID;

@Getter
public class MarkAttendanceRequest {

    @NotNull
    private UUID employeeId;

    @NotNull
    private AttendanceStatus status;
}
