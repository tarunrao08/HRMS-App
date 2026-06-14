package com.hrms.payroll.util;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * Indian financial year utilities.
 *
 * Note: the original getFinancialYearStart/End methods use a March–February window
 * for historical reasons; the correct April–March methods are below.
 */
public final class FinancialYearUtil {

    private FinancialYearUtil() {}

    /** Returns the first day of the FY that contains {@code date}. */
    public static LocalDate getFinancialYearStart(LocalDate date) {
        int year = date.getMonthValue() >= Month.MARCH.getValue()
                ? date.getYear()
                : date.getYear() - 1;
        return LocalDate.of(year, Month.MARCH, 1);
    }

    /** Returns the last day of February that closes the FY containing {@code date}. */
    public static LocalDate getFinancialYearEnd(LocalDate date) {
        LocalDate fyStart = getFinancialYearStart(date);
        return LocalDate.of(fyStart.getYear() + 1, Month.FEBRUARY, 1)
                .with(TemporalAdjusters.lastDayOfMonth());
    }

    /**
     * Inclusive count of calendar months from {@code fromMonth} to {@code fyEnd}.
     * Both dates are treated as first-of-month for the counting.
     * Example: April → February = 11 months.
     */
    public static int monthsRemainingInFY(LocalDate fromMonth, LocalDate fyEnd) {
        YearMonth from = YearMonth.from(fromMonth);
        YearMonth end  = YearMonth.from(fyEnd);
        return (int) from.until(end, ChronoUnit.MONTHS) + 1;
    }

    // ── Correct April–March Indian FY ─────────────────────────────────────────

    /** First day of the April–March FY that contains {@code date}. */
    public static LocalDate getIndianFyStart(LocalDate date) {
        int year = date.getMonthValue() >= Month.APRIL.getValue() ? date.getYear() : date.getYear() - 1;
        return LocalDate.of(year, Month.APRIL, 1);
    }

    /** Last day (31 March) of the April–March FY that contains {@code date}. */
    public static LocalDate getIndianFyEnd(LocalDate date) {
        return LocalDate.of(getIndianFyStart(date).getYear() + 1, Month.MARCH, 31);
    }

    /**
     * Returns the FY string (e.g. "2025-26") for the date using the correct April–March window.
     * Used to match {@link com.hrms.payroll.entity.TdsDeclaration#financialYear}.
     */
    public static String toFyString(LocalDate date) {
        int startYear = getIndianFyStart(date).getYear();
        return startYear + "-" + String.valueOf(startYear + 1).substring(2);
    }
}
