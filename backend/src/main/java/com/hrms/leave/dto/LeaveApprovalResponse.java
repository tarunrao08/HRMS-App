package com.hrms.leave.dto;

import com.hrms.leave.enums.LeaveApprovalAction;
import com.hrms.leave.enums.LeaveApprovalStatus;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class LeaveApprovalResponse {

    private UUID id;
    private UUID leaveRequestId;
    private UUID approverId;
    private String approverName;
    private int approverLevel;
    private LeaveApprovalStatus status;
    private LeaveApprovalAction action;
    private String comments;
    private Instant actedAt;
}
