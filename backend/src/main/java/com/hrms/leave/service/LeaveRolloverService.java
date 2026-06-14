package com.hrms.leave.service;

/**
 * Handles the annual leave rollover: allocates the new year's balances and applies carry-forward
 * from the previous year based on each leave type's configuration.
 */
public interface LeaveRolloverService {

    /**
     * Runs the annual rollover for the given target year.
     * For every employee who had a balance in (year - 1):
     * <ul>
     *   <li>Allocates a fresh balance for {@code year} (via AllocationService)</li>
     *   <li>Adds carry-forward from the previous year where the leave type allows it</li>
     * </ul>
     *
     * @param year the target year for which to create balances
     * @return the number of employees successfully processed
     */
    int runRollover(int year);
}
