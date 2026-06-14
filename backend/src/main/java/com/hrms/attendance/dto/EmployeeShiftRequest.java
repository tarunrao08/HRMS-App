package com.hrms.attendance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class EmployeeShiftRequest {

    @NotNull
    private UUID employeeId;

    @NotNull
    private UUID shiftId;

    @NotNull
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;
}
