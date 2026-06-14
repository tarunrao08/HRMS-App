package com.hrms.leave.dto;

import com.hrms.leave.enums.LeaveApprovalAction;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LeaveApprovalRequest {

    private LeaveApprovalAction action;

    private String comments;
}
