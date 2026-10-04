package com.hrms.leave.service;

import com.hrms.leave.dto.AdjustLeaveBalanceRequest;
import com.hrms.leave.dto.LeaveBalanceAdjustmentResponse;
import com.hrms.leave.dto.LeaveBalanceResponse;

import java.util.List;
import java.util.UUID;

public interface LeaveBalanceService {

    List<LeaveBalanceResponse> getBalances(UUID employeeId, int year);

    /** Same as getBalances, but if the caller isn't HR Admin they must be the target employee's direct manager. */
    List<LeaveBalanceResponse> getBalancesScoped(UUID employeeId, int year, UUID actingEmployeeId, boolean isHrAdmin);

    LeaveBalanceResponse adjustBalance(UUID employeeId, UUID actingEmployeeId, boolean isHrAdmin, AdjustLeaveBalanceRequest request);

    List<LeaveBalanceAdjustmentResponse> getAdjustmentHistory(UUID employeeId, UUID leaveTypeId, int year);
}
