package com.hrms.attendance.service;

import com.hrms.attendance.entity.AttendanceDayLedger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Maintains the per-day, per-half attendance/leave ledger — payroll's single source of
 * truth for paid-days / loss-of-pay computation. See AttendanceDayLedger for the model.
 */
public interface AttendanceDayLedgerService {

    /** Reconciles and upserts the ledger row for a single date. */
    AttendanceDayLedger reconcileDate(UUID employeeId, LocalDate date);

    /** Reconciles and upserts the ledger rows for every date in [from, to] (inclusive). */
    List<AttendanceDayLedger> reconcileRange(UUID employeeId, LocalDate from, LocalDate to);

    /** Total loss-of-pay days (fractional, in halves) for [from, to] (inclusive), self-healing the ledger as needed. */
    BigDecimal getLopDays(UUID employeeId, LocalDate from, LocalDate to);
}
