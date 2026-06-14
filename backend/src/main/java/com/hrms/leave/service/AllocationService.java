package com.hrms.leave.service;

import java.util.UUID;

/**
 * Allocates leave balances for an employee for a given calendar year.
 * The current implementation (Option A) uses fixed quantities from leave_types.max_days_per_year.
 * A future implementation could read from a leave_policies table without changing this interface.
 */
public interface AllocationService {

    /**
     * Creates one leave_balances row per active leave type for the given employee and year.
     * Silently skips any leave type for which a balance already exists (no exception thrown).
     *
     * @param employeeId the employee to allocate for
     * @param year       the calendar year
     */
    void allocateLeaveForEmployee(UUID employeeId, int year);
}
