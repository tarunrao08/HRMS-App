package com.hrms.leave.service.impl;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.leave.entity.LeaveBalance;
import com.hrms.leave.entity.LeaveType;
import com.hrms.leave.entity.LeaveTypeTenureTier;
import com.hrms.leave.repository.LeaveBalanceRepository;
import com.hrms.leave.repository.LeaveTypeRepository;
import com.hrms.leave.repository.LeaveTypeTenureTierRepository;
import com.hrms.leave.service.AllocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AllocationServiceImpl implements AllocationService {

    private final EmployeeRepository              employeeRepository;
    private final LeaveTypeRepository             leaveTypeRepository;
    private final LeaveBalanceRepository          leaveBalanceRepository;
    private final LeaveTypeTenureTierRepository   leaveTypeTenureTierRepository;

    @Override
    @Transactional
    public void allocateLeaveForEmployee(UUID employeeId, int year) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId.toString()));

        List<LeaveType> activeTypes = leaveTypeRepository.findByActiveTrue();

        for (LeaveType leaveType : activeTypes) {
            if (leaveType.getApplicableGender() != null && leaveType.getApplicableGender() != employee.getGender()) {
                log.debug("Leave type {} is restricted to {} — skipping for employee={} (gender={})",
                        leaveType.getCode(), leaveType.getApplicableGender(), employeeId, employee.getGender());
                continue;
            }

            boolean exists = leaveBalanceRepository
                    .findByEmployeeIdAndLeaveTypeIdAndYear(employeeId, leaveType.getId(), year)
                    .isPresent();

            if (exists) {
                log.debug("Balance already exists for employee={} leaveType={} year={} — skipping",
                        employeeId, leaveType.getCode(), year);
                continue;
            }

            int allocatedDays = resolveAllocatedDays(leaveType, employee, year);

            LeaveBalance balance = LeaveBalance.builder()
                    .employee(employee)
                    .leaveType(leaveType)
                    .year(year)
                    .allocatedDays(BigDecimal.valueOf(allocatedDays))
                    .usedDays(BigDecimal.ZERO)
                    .pendingDays(BigDecimal.ZERO)
                    .carriedForwardDays(BigDecimal.ZERO)
                    .lapsedDays(BigDecimal.ZERO)
                    .build();

            leaveBalanceRepository.save(balance);
            log.info("Allocated {} days of {} to employee {} for year {}",
                    allocatedDays, leaveType.getCode(), employeeId, year);
        }
    }

    /** Flat maxDaysPerYear, unless the leave type is tenure-based, in which case the matching tier wins. */
    private int resolveAllocatedDays(LeaveType leaveType, Employee employee, int year) {
        if (!leaveType.isTenureBased()) {
            return leaveType.getMaxDaysPerYear();
        }

        int completedYears = Math.max(0,
                Period.between(employee.getJoiningDate(), LocalDate.of(year, 1, 1)).getYears());

        return leaveTypeTenureTierRepository.findMatchingTier(leaveType.getId(), completedYears)
                .map(LeaveTypeTenureTier::getDays)
                .orElseGet(() -> {
                    log.warn("No tenure tier matches {} completed years for leave type {} — falling back to maxDaysPerYear",
                            completedYears, leaveType.getCode());
                    return leaveType.getMaxDaysPerYear();
                });
    }
}
