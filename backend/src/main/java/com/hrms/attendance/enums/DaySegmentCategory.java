package com.hrms.attendance.enums;

/**
 * Classification of a half-day (AM/PM) segment in the attendance day ledger.
 * This is the single vocabulary payroll uses to compute paid vs. loss-of-pay days —
 * new attendance statuses or leave types only ever need to map into one of these,
 * so the payroll aggregation logic itself never has to change.
 */
public enum DaySegmentCategory {
    WORKED,
    LEAVE,
    ABSENT,
    WEEKEND,
    HOLIDAY,
    UNMARKED
}
