package com.hrms.leave.service.impl;

import com.hrms.leave.entity.LeaveBalance;
import com.hrms.leave.entity.LeaveType;
import com.hrms.leave.repository.LeaveBalanceRepository;
import com.hrms.leave.service.AllocationService;
import com.hrms.leave.service.LeaveRolloverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveRolloverServiceImpl implements LeaveRolloverService {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final AllocationService      allocationService;

    @Override
    public int runRollover(int year) {
        int prevYear = year - 1;
        log.info("Starting annual leave rollover: prev={} -> new={}", prevYear, year);

        List<LeaveBalance> prevBalances = leaveBalanceRepository.findByYear(prevYear);
        if (prevBalances.isEmpty()) {
            log.info("No leave balances found for year {}, nothing to roll over", prevYear);
            return 0;
        }

        // Group previous-year balances by employee
        Map<UUID, List<LeaveBalance>> byEmployee = prevBalances.stream()
                .collect(Collectors.groupingBy(b -> b.getEmployee().getId()));

        int successCount = 0;
        for (Map.Entry<UUID, List<LeaveBalance>> entry : byEmployee.entrySet()) {
            UUID empId = entry.getKey();
            List<LeaveBalance> empPrev = entry.getValue();
            try {
                processEmployee(empId, empPrev, year);
                successCount++;
            } catch (Exception e) {
                log.error("Rollover failed for employee {}: {}", empId, e.getMessage(), e);
            }
        }

        log.info("Annual leave rollover for year {} complete — processed {}/{} employees",
                year, successCount, byEmployee.size());
        return successCount;
    }

    /**
     * Allocates the new year's balances for one employee then applies carry-forward per leave type.
     * Each call runs in its own transaction via AllocationService.
     */
    private void processEmployee(UUID empId, List<LeaveBalance> prevBalances, int year) {
        // Step 1 — allocate base balances for the new year (skips silently if already present)
        allocationService.allocateLeaveForEmployee(empId, year);

        // Step 2 — apply carry-forward for eligible leave types
        for (LeaveBalance prev : prevBalances) {
            LeaveType lt = prev.getLeaveType();
            if (!lt.isCarryForwardAllowed() || lt.getMaxCarryForwardDays() <= 0) {
                continue;
            }

            BigDecimal remaining = prev.getAllocatedDays()
                    .add(prev.getCarriedForwardDays())
                    .subtract(prev.getUsedDays())
                    .subtract(prev.getPendingDays());

            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal carryForward = remaining.min(BigDecimal.valueOf(lt.getMaxCarryForwardDays()));

            leaveBalanceRepository
                    .findByEmployeeIdAndLeaveTypeIdAndYear(empId, lt.getId(), year)
                    .ifPresent(newBalance -> {
                        newBalance.setCarriedForwardDays(
                                newBalance.getCarriedForwardDays().add(carryForward));
                        leaveBalanceRepository.save(newBalance);
                        log.debug("Carried forward {} days of {} for employee {} into year {}",
                                carryForward, lt.getCode(), empId, year);
                    });
        }
    }
}
