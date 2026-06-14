package com.hrms.payroll.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDS calculation under the simplified new-tax-regime:
 *   - ₹0 – ₹12,00,000 annual taxable → 0% tax
 *   - Above ₹12,00,000              → 10% on the excess
 *
 * Financial year: 1 March → last day of February.
 */
class TaxCalculatorTest {

    private TaxCalculator taxCalculator;

    @BeforeEach
    void setUp() {
        taxCalculator = new TaxCalculator();
    }

    // ── Test 1: Full-year employee, gross > 12L annually ─────────────────────
    // Joining date is before the FY; FY start (March) is the effective start.
    // monthlyGross = ₹1,20,000  →  annual = ₹14,40,000  →  excess = ₹2,40,000
    // annualTax = ₹24,000  →  monthlyTds = ₹2,000.00
    @Test
    @DisplayName("Full-year employee with annual income above ₹12L pays 10% on excess")
    void fullYear_incomeAbove12L_returnsCorrectMonthlyTds() {
        BigDecimal monthlyGross = new BigDecimal("120000");
        LocalDate joiningDate   = LocalDate.of(2023, 1, 10);   // well before FY
        LocalDate payrollMonth  = LocalDate.of(2024, 3, 1);     // first month of FY 2024-25

        BigDecimal result = taxCalculator.calculateMonthlyTds(monthlyGross, joiningDate, payrollMonth);

        assertThat(result).isEqualByComparingTo(new BigDecimal("2000.00"));
    }

    // ── Test 2: Full-year employee, gross <= 12L annually ────────────────────
    // monthlyGross = ₹90,000  →  annual = ₹10,80,000  ≤  ₹12,00,000  →  tax = 0
    @Test
    @DisplayName("Full-year employee with annual income at or below ₹12L pays no TDS")
    void fullYear_incomeBelow12L_returnsZero() {
        BigDecimal monthlyGross = new BigDecimal("90000");
        LocalDate joiningDate   = LocalDate.of(2023, 1, 10);
        LocalDate payrollMonth  = LocalDate.of(2024, 3, 1);

        BigDecimal result = taxCalculator.calculateMonthlyTds(monthlyGross, joiningDate, payrollMonth);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── Test 3: Mid-FY joiner (December), gross > 12L annually but only ──────
    //           3 months remain in FY so pro-rated taxable < 12L → tax = 0
    // Joins December 15, 2024; payroll month = December 2024.
    // Months remaining (Dec → Feb inclusive) = 3
    // monthlyGross = ₹1,50,000  →  3-month taxable = ₹4,50,000  ≤  ₹12,00,000
    @Test
    @DisplayName("Mid-FY December joiner with 3 remaining months pays no TDS even at high salary")
    void midFY_decemberJoiner_proRatedIncomeBelow12L_returnsZero() {
        BigDecimal monthlyGross = new BigDecimal("150000");
        LocalDate joiningDate   = LocalDate.of(2024, 12, 15);
        LocalDate payrollMonth  = LocalDate.of(2024, 12, 1);

        BigDecimal result = taxCalculator.calculateMonthlyTds(monthlyGross, joiningDate, payrollMonth);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── Test 4: Mid-FY joiner (September), gross high enough that even ────────
    //           the 6 remaining months exceed 12L → TDS applies
    // Joins September 15, 2024; payroll month = September 2024.
    // Months remaining (Sep → Feb inclusive) = 6
    // monthlyGross = ₹2,50,000  →  6-month taxable = ₹15,00,000
    // annualTax = (₹15,00,000 - ₹12,00,000) × 10% = ₹30,000
    // monthlyTds = ₹30,000 / 6 = ₹5,000.00
    @Test
    @DisplayName("Mid-FY September joiner with high salary pays TDS on remaining 6 months")
    void midFY_septemberJoiner_proRatedIncomeAbove12L_returnsCorrectTds() {
        BigDecimal monthlyGross = new BigDecimal("250000");
        LocalDate joiningDate   = LocalDate.of(2024, 9, 15);
        LocalDate payrollMonth  = LocalDate.of(2024, 9, 1);

        BigDecimal result = taxCalculator.calculateMonthlyTds(monthlyGross, joiningDate, payrollMonth);

        assertThat(result).isEqualByComparingTo(new BigDecimal("5000.00"));
    }
}
