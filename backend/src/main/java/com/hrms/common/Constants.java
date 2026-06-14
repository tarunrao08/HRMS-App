// Application-wide string constants — never inline magic strings in service or controller code
package com.hrms.common;

public final class Constants {

    private Constants() {}

    public static final class Roles {
        public static final String HR_ADMIN = "ROLE_HR_ADMIN";
        public static final String MANAGER  = "ROLE_MANAGER";
        public static final String EMPLOYEE = "ROLE_EMPLOYEE";
    }

    public static final class ErrorCodes {
        public static final String RESOURCE_NOT_FOUND       = "RESOURCE_NOT_FOUND";
        public static final String VALIDATION_ERROR         = "VALIDATION_ERROR";
        public static final String BUSINESS_RULE_VIOLATION  = "BUSINESS_RULE_VIOLATION";
        public static final String UNAUTHORIZED_ACCESS      = "UNAUTHORIZED_ACCESS";
        public static final String ACCESS_DENIED            = "ACCESS_DENIED";
        public static final String AUTHENTICATION_FAILED    = "AUTHENTICATION_FAILED";
        public static final String INTERNAL_SERVER_ERROR    = "INTERNAL_SERVER_ERROR";
        public static final String INVALID_TOKEN            = "INVALID_TOKEN";
        public static final String TOKEN_EXPIRED            = "TOKEN_EXPIRED";
        public static final String INVALID_CREDENTIALS      = "INVALID_CREDENTIALS";
        public static final String USER_DISABLED            = "USER_DISABLED";
        public static final String DUPLICATE_RESOURCE       = "DUPLICATE_RESOURCE";
        public static final String PAYROLL_ALREADY_PROCESSED = "PAYROLL_ALREADY_PROCESSED";
        public static final String LEAVE_BALANCE_INSUFFICIENT = "LEAVE_BALANCE_INSUFFICIENT";
        public static final String ATTENDANCE_ALREADY_MARKED  = "ATTENDANCE_ALREADY_MARKED";
    }

    public static final class DateFormats {
        public static final String DATE         = "yyyy-MM-dd";
        public static final String DATETIME     = "yyyy-MM-dd HH:mm:ss";
        public static final String MONTH_YEAR   = "MM-yyyy";
    }

    public static final class EmploymentStatus {
        public static final String ACTIVE     = "ACTIVE";
        public static final String RESIGNED   = "RESIGNED";
        public static final String TERMINATED = "TERMINATED";
        public static final String ON_LEAVE   = "ON_LEAVE";
        public static final String PROBATION  = "PROBATION";
    }

    public static final class EmploymentType {
        public static final String FULL_TIME = "FULL_TIME";
        public static final String PART_TIME = "PART_TIME";
        public static final String CONTRACT  = "CONTRACT";
        public static final String INTERN    = "INTERN";
    }

    public static final class AttendanceStatus {
        public static final String PRESENT         = "PRESENT";
        public static final String ABSENT          = "ABSENT";
        public static final String HALF_DAY        = "HALF_DAY";
        public static final String ON_LEAVE        = "ON_LEAVE";
        public static final String HOLIDAY         = "HOLIDAY";
        public static final String WEEKEND         = "WEEKEND";
        public static final String LATE            = "LATE";
        public static final String WORK_FROM_HOME  = "WORK_FROM_HOME";
    }

    public static final class LeaveStatus {
        public static final String PENDING   = "PENDING";
        public static final String APPROVED  = "APPROVED";
        public static final String REJECTED  = "REJECTED";
        public static final String CANCELLED = "CANCELLED";
        public static final String WITHDRAWN = "WITHDRAWN";
    }

    public static final class PayrollStatus {
        public static final String DRAFT      = "DRAFT";
        public static final String PROCESSING = "PROCESSING";
        public static final String PROCESSED  = "PROCESSED";
        public static final String APPROVED   = "APPROVED";
        public static final String DISBURSED  = "DISBURSED";
    }

    public static final class OnboardingStatus {
        public static final String NOT_STARTED = "NOT_STARTED";
        public static final String IN_PROGRESS = "IN_PROGRESS";
        public static final String COMPLETED   = "COMPLETED";
        public static final String ON_HOLD     = "ON_HOLD";
    }

    public static final class Payroll {
        public static final double PF_RATE              = 0.12;
        public static final double ESI_EMPLOYEE_RATE    = 0.0075;
        public static final double ESI_EMPLOYER_RATE    = 0.0325;
        public static final double ESI_WAGE_CEILING     = 21_000.0;
        public static final double PF_WAGE_CEILING      = 15_000.0;
        public static final double PROFESSIONAL_TAX_PM  = 200.0;
        public static final double BASIC_PCT_OF_CTC     = 0.40;
        public static final double HRA_PCT_OF_BASIC     = 0.50;
    }

    public static final class Security {
        public static final String TOKEN_PREFIX  = "Bearer ";
        public static final String AUTH_HEADER   = "Authorization";
        public static final int    BCRYPT_ROUNDS = 12;
    }
}
