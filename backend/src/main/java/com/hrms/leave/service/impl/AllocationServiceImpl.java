package com.hrms.leave.service.impl;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.leave.entity.LeaveBalance;
import com.hrms.leave.entity.LeaveType;
import com.hrms.leave.repository.LeaveBalanceRepository;
import com.hrms.leave.repository.LeaveTypeRepository;
import com.hrms.leave.service.AllocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AllocationServiceImpl implements AllocationService {

    private final EmployeeRepository      employeeRepository;
    private final LeaveTypeRepository     leaveTypeRepository;
    private final LeaveBalanceRepository  leaveBalanceRepository;

    @Override
    @Transactional
    public void allocateLeaveForEmployee(UUID employeeId, int year) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId.toString()));

        List<LeaveType> activeTypes = leaveTypeRepository.findByActiveTrue();

        for (LeaveType leaveType : activeTypes) {
            boolean exists = leaveBalanceRepository
                    .findByEmployeeIdAndLeaveTypeIdAndYear(employeeId, leaveType.getId(), year)
                    .isPresent();

            if (exists) {
                log.debug("Balance already exists for employee={} leaveType={} year={} — skipping",
                        employeeId, leaveType.getCode(), year);
                continue;
            }

            LeaveBalance balance = LeaveBalance.builder()
                    .employee(employee)
                    .leaveType(leaveType)
                    .year(year)
                    .allocatedDays(BigDecimal.valueOf(leaveType.getMaxDaysPerYear()))
                    .usedDays(BigDecimal.ZERO)
                    .pendingDays(BigDecimal.ZERO)
                    .carriedForwardDays(BigDecimal.ZERO)
                    .lapsedDays(BigDecimal.ZERO)
                    .build();

            leaveBalanceRepository.save(balance);
            log.info("Allocated {} days of {} to employee {} for year {}",
                    leaveType.getMaxDaysPerYear(), leaveType.getCode(), employeeId, year);
        }
    }
}
