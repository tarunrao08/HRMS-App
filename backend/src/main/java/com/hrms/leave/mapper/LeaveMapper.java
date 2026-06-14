package com.hrms.leave.mapper;

import com.hrms.leave.dto.LeaveApprovalResponse;
import com.hrms.leave.dto.LeaveBalanceResponse;
import com.hrms.leave.dto.LeaveRequestResponse;
import com.hrms.leave.entity.LeaveApproval;
import com.hrms.leave.entity.LeaveBalance;
import com.hrms.leave.entity.LeaveRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LeaveMapper {

    @Mapping(source = "employee.id",           target = "employeeId")
    @Mapping(source = "employee.employeeCode", target = "employeeCode")
    @Mapping(source = "leaveType.id",          target = "leaveTypeId")
    @Mapping(source = "leaveType.name",        target = "leaveTypeName")
    @Mapping(source = "leaveType.code",        target = "leaveTypeCode")
    @Mapping(source = "currentApprover.id",    target = "currentApproverId")
    @Mapping(target = "employeeName",
             expression = "java(request.getEmployee().getFirstName() + ' ' + request.getEmployee().getLastName())")
    @Mapping(target = "currentApproverName",
             expression = "java(request.getCurrentApprover() != null ? request.getCurrentApprover().getFirstName() + ' ' + request.getCurrentApprover().getLastName() : null)")
    LeaveRequestResponse toRequestResponse(LeaveRequest request);

    @Mapping(source = "employee.id",        target = "employeeId")
    @Mapping(source = "leaveType.id",       target = "leaveTypeId")
    @Mapping(source = "leaveType.name",     target = "leaveTypeName")
    @Mapping(source = "leaveType.code",     target = "leaveTypeCode")
    @Mapping(target = "availableDays",
             expression = "java(balance.getAllocatedDays().add(balance.getCarriedForwardDays()).subtract(balance.getUsedDays()).subtract(balance.getPendingDays()))")
    LeaveBalanceResponse toBalanceResponse(LeaveBalance balance);

    @Mapping(source = "leaveRequest.id",  target = "leaveRequestId")
    @Mapping(source = "approver.id",      target = "approverId")
    @Mapping(target = "approverName",
             expression = "java(approval.getApprover().getFirstName() + ' ' + approval.getApprover().getLastName())")
    LeaveApprovalResponse toApprovalResponse(LeaveApproval approval);
}
