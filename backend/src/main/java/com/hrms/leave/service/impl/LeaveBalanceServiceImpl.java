package com.hrms.leave.service.impl;

import com.hrms.common.exception.AppException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.exception.ValidationException;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.leave.dto.AdjustLeaveBalanceRequest;
import com.hrms.leave.dto.LeaveBalanceAdjustmentResponse;
import com.hrms.leave.dto.LeaveBalanceResponse;
import com.hrms.leave.entity.LeaveBalance;
import com.hrms.leave.entity.LeaveBalanceAdjustment;
import com.hrms.leave.mapper.LeaveMapper;
import com.hrms.leave.repository.LeaveBalanceAdjustmentRepository;
import com.hrms.leave.repository.LeaveBalanceRepository;
import com.hrms.leave.service.LeaveBalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveBalanceServiceImpl implements LeaveBalanceService {

    private final LeaveBalanceRepository           leaveBalanceRepository;
    private final LeaveBalanceAdjustmentRepository leaveBalanceAdjustmentRepository;
    private final EmployeeRepository               employeeRepository;
    private final LeaveMapper                      leaveMapper;

    @Override
    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> getBalances(UUID employeeId, int year) {
        return leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, year).stream()
                .map(leaveMapper::toBalanceResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> getBalancesScoped(UUID employeeId, int year, UUID actingEmployeeId, boolean isHrAdmin) {
        if (!isHrAdmin) {
            requireDirectManager(employeeId, actingEmployeeId);
        }
        return getBalances(employeeId, year);
    }

    @Override
    @Transactional
    public LeaveBalanceResponse adjustBalance(UUID employeeId, UUID actingEmployeeId, boolean isHrAdmin,
                                               AdjustLeaveBalanceRequest request) {
        if (!isHrAdmin) {
            requireDirectManager(employeeId, actingEmployeeId);
        }

        LeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndYear(employeeId, request.getLeaveTypeId(), request.getYear())
                .orElseThrow(() -> new ResourceNotFoundException("LeaveBalance", "employeeId/leaveTypeId/year",
                        employeeId + "/" + request.getLeaveTypeId() + "/" + request.getYear()));

        BigDecimal newAllocated = balance.getAllocatedDays().add(request.getDays());
        BigDecimal committed = balance.getUsedDays().add(balance.getPendingDays());
        if (newAllocated.compareTo(committed) < 0) {
            throw new ValidationException(
                    "Cannot reduce allocated days below what's already used/pending (" + committed + ")");
        }

        balance.setAllocatedDays(newAllocated);
        balance = leaveBalanceRepository.save(balance);

        Employee adjustedBy = employeeRepository.findById(actingEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id",
                        String.valueOf(actingEmployeeId)));

        LeaveBalanceAdjustment adjustment = LeaveBalanceAdjustment.builder()
                .leaveBalance(balance)
                .days(request.getDays())
                .reason(request.getReason())
                .adjustedBy(adjustedBy)
                .build();
        leaveBalanceAdjustmentRepository.save(adjustment);

        log.info("Adjusted leave balance {} by {} days for employee {} (reason: {})",
                balance.getId(), request.getDays(), employeeId, request.getReason());

        return leaveMapper.toBalanceResponse(balance);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveBalanceAdjustmentResponse> getAdjustmentHistory(UUID employeeId, UUID leaveTypeId, int year) {
        LeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndYear(employeeId, leaveTypeId, year)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveBalance", "employeeId/leaveTypeId/year",
                        employeeId + "/" + leaveTypeId + "/" + year));
        return leaveBalanceAdjustmentRepository.findByLeaveBalanceIdOrderByCreatedAtDesc(balance.getId()).stream()
                .map(leaveMapper::toAdjustmentResponse)
                .toList();
    }

    private void requireDirectManager(UUID employeeId, UUID actingEmployeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId.toString()));
        Employee manager = employee.getManager();
        if (manager == null || actingEmployeeId == null || !manager.getId().equals(actingEmployeeId)) {
            throw new AppException(
                    "You can only view or adjust leave balances for your own direct reports",
                    HttpStatus.FORBIDDEN, "NOT_DIRECT_MANAGER");
        }
    }
}
