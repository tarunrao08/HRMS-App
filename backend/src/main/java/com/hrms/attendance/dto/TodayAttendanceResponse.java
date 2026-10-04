package com.hrms.attendance.dto;

import com.hrms.attendance.enums.AttendanceStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class TodayAttendanceResponse {
    private UUID employeeId;
    private String employeeName;
    private String employeeCode;
    private String email;
    private UUID departmentId;
    private String departmentName;
    private String designationTitle;
    private UUID shiftId;
    private String shiftName;
    private AttendanceStatus status;
    private UUID recordId;
}
