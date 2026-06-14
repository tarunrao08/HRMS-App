package com.hrms.attendance.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class EmployeeShiftResponse {

    private UUID id;
    private UUID employeeId;
    private String employeeName;
    private UUID shiftId;
    private String shiftName;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
}
