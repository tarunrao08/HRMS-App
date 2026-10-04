package com.hrms.attendance.enums;

public enum AttendanceStatus {
    PRESENT,
    ABSENT,
    LATE,
    HALF_DAY,
    HOLIDAY,
    WEEKEND,
    ON_LEAVE,
    WORK_FROM_HOME,
    REGULARIZED,
    /** No attendance record exists yet for the day — never persisted, only returned by read APIs. */
    NOT_MARKED
}
