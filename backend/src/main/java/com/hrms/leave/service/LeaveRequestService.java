package com.hrms.leave.service;

import com.hrms.common.dto.PageableResponse;
import com.hrms.leave.dto.*;
import com.hrms.leave.enums.LeaveRequestStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface LeaveRequestService {

    LeaveRequestResponse submit(UUID employeeId, ApplyLeaveRequest request);

    PageableResponse<LeaveRequestResponse> getMyRequests(UUID employeeId, LeaveRequestStatus status, Pageable pageable);

    List<LeaveRequestResponse> getPendingApprovals(UUID approverId);

    List<LeaveRequestResponse> getPendingApprovalsForHrAdmin();

    List<LeaveRequestResponse> getPendingForManager(UUID managerId);

    LeaveRequestResponse approve(UUID requestId, UUID approverId, ApproveRejectRequest request);

    LeaveRequestResponse reject(UUID requestId, UUID approverId, ApproveRejectRequest request);

    LeaveRequestResponse cancel(UUID requestId, UUID cancelledByUserId);

    PageableResponse<LeaveRequestResponse> getAllRequests(UUID employeeId, LeaveRequestStatus status, Pageable pageable);
}
