package com.hrms.attendance.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class AttendanceMonthlySummaryResponse {

    private UUID id;
    private UUID employeeId;
    private String employeeName;
    private int year;
    private int month;
    private int presentDays;
    private int absentDays;
    private int lateDays;
    private int halfDays;
    private BigDecimal overtimeHours;
    private BigDecimal totalWorkingHours;
    private int workingDays;
    private int totalWorkingDays;
}
