package com.hrms.attendance.dto;

import com.hrms.attendance.enums.AttendanceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
public class RegularizationRequest {

    @NotNull
    private LocalDate attendanceDate;

    @NotNull
    private AttendanceStatus status;

    private Instant punchIn;
    private Instant punchOut;

    @NotBlank
    private String reason;
}
