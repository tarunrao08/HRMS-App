package com.hrms.attendance.dto;

import com.hrms.attendance.enums.AttendanceStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class AttendanceRecordResponse {

    private UUID id;
    private UUID employeeId;
    private String employeeName;
    private String employeeCode;
    private LocalDate attendanceDate;
    private Instant punchIn;
    private Instant punchOut;
    private BigDecimal workingHours;
    private BigDecimal overtimeHours;
    private AttendanceStatus status;
    private String punchInLocation;
    private String punchOutLocation;
    private String punchInIp;
    private String punchOutIp;
    private UUID shiftId;
    private String shiftName;
    private String remarks;
    private boolean regularized;
    private String regularizationReason;
    private UUID regularizedBy;
    private Instant regularizedAt;
    private Instant createdAt;
}
