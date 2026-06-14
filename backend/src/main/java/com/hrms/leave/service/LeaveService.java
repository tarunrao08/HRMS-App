package com.hrms.leave.service;

import com.hrms.common.dto.PageableResponse;
import com.hrms.leave.dto.*;
import com.hrms.leave.enums.LeaveRequestStatus;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface LeaveService {

    LeaveRequestResponse apply(LeaveRequestRequest request);

    LeaveRequestResponse approve(UUID requestId, UUID approverId, LeaveApprovalRequest request);

    LeaveRequestResponse reject(UUID requestId, UUID approverId, LeaveApprovalRequest request);

    LeaveRequestResponse cancel(UUID requestId, UUID cancelledByUserId);

    List<LeaveBalanceResponse> getBalances(UUID employeeId, int year);

    LeaveBalanceResponse allocateBalance(UUID employeeId, UUID leaveTypeId, int year, BigDecimal days);

    PageableResponse<LeaveRequestResponse> getRequests(UUID employeeId, LeaveRequestStatus status, Pageable pageable);

    PageableResponse<LeaveRequestResponse> getAllRequests(UUID employeeId, LeaveRequestStatus status, Pageable pageable);

    List<LeaveRequestResponse> getPendingForApprover(UUID approverId);

    List<LeaveApprovalResponse> getApprovals(UUID requestId);
}
