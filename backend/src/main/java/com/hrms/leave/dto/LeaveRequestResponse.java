package com.hrms.leave.dto;

import com.hrms.leave.enums.HalfDayType;
import com.hrms.leave.enums.LeaveRequestStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class LeaveRequestResponse {

    private UUID id;
    private UUID employeeId;
    private String employeeName;
    private String employeeCode;
    private UUID leaveTypeId;
    private String leaveTypeName;
    private String leaveTypeCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalDays;
    private boolean halfDay;
    private HalfDayType halfDayType;
    private String reason;
    private LeaveRequestStatus status;
    private Instant appliedAt;
    private String documentUrl;
    private int currentApprovalLevel;
    private UUID currentApproverId;
    private String currentApproverName;
    private String rejectionReason;
    private Instant cancelledAt;
    private Instant createdAt;
}
