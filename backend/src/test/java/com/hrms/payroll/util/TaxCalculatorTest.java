package com.hrms.payroll.util;

import com.hrms.payroll.enums.TaxRegime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDS calculation under the new tax regime (no declaration), FY 2024-25 slabs:
 *   Std. deduction ₹75,000; slabs 0-3L 0%, 3-7L 5%, 7-10L 10%, 10-12L 15%, 12-15L 20%, 15L+ 30%;
 *   87A rebate → ₹0 tax if taxable income ≤ ₹7,00,000; 4% Health & Education Cess on top.
 *
 * Financial year: 1 April → 31 March (see FinancialYearUtil.getIndianFyStart/End).
 */
class TaxCalculatorTest {

    private TaxCalculator taxCalculator;

    @BeforeEach
    void setUp() {
        taxCalculator = new TaxCalculator();
    }

    // ── Test 1: Full-year employee, taxable income crosses into the 5%/10% slabs ─
    // Joining date is before the FY; FY start (April) is the effective start → 12 months.
    // monthlyGross = ₹80,000 → annual = ₹9,60,000 → taxable = ₹8,85,000 (after ₹75,000 std. deduction)
    // slab tax = 400,000×5% + 185,000×10% = ₹38,500 → +4% cess = ₹40,040 → monthlyTds = ₹3,336.67
    @Test
    @DisplayName("Full-year employee with taxable income in the 5%/10% slabs pays cess-inclusive TDS")
    void fullYear_incomeInMiddleSlabs_returnsCorrectMonthlyTds() {
        BigDecimal monthlyGross = new BigDecimal("80000");
        LocalDate joiningDate   = LocalDate.of(2023, 1, 10);   // well before FY
        LocalDate payrollMonth  = LocalDate.of(2024, 4, 1);     // first month of FY 2024-25

        BigDecimal result = taxCalculator.calculateMonthlyTds(
                monthlyGross, joiningDate, payrollMonth, TaxRegime.NEW, Optional.empty());

        assertThat(result).isEqualByComparingTo(new BigDecimal("3336.67"));
    }

    // ── Test 2: Full-year employee, taxable income at or below the ₹7L 87A rebate ─
    // monthlyGross = ₹50,000 → annual = ₹6,00,000 → taxable = ₹5,25,000 ≤ ₹7,00,000 → tax = 0
    @Test
    @DisplayName("Full-year employee with taxable income at or below the 87A rebate limit pays no TDS")
    void fullYear_incomeAtOrBelowRebateLimit_returnsZero() {
        BigDecimal monthlyGross = new BigDecimal("50000");
        LocalDate joiningDate   = LocalDate.of(2023, 1, 10);
        LocalDate payrollMonth  = LocalDate.of(2024, 4, 1);

        BigDecimal result = taxCalculator.calculateMonthlyTds(
                monthlyGross, joiningDate, payrollMonth, TaxRegime.NEW, Optional.empty());

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── Test 3: Mid-FY joiner (December), pro-rated taxable income stays under ───
    //           the rebate limit even though the full-year run rate would exceed it.
    // Joins December 15, 2024; payroll month = December 2024.
    // Months remaining (Dec → Mar inclusive, FY ends 31 March) = 4
    // monthlyGross = ₹1,50,000 → 4-month taxable = ₹6,00,000 - ₹75,000 = ₹5,25,000 ≤ ₹7,00,000 → tax = 0
    @Test
    @DisplayName("Mid-FY December joiner with pro-rated income under the rebate limit pays no TDS")
    void midFY_decemberJoiner_proRatedIncomeUnderRebateLimit_returnsZero() {
        BigDecimal monthlyGross = new BigDecimal("150000");
        LocalDate joiningDate   = LocalDate.of(2024, 12, 15);
        LocalDate payrollMonth  = LocalDate.of(2024, 12, 1);

        BigDecimal result = taxCalculator.calculateMonthlyTds(
                monthlyGross, joiningDate, payrollMonth, TaxRegime.NEW, Optional.empty());

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── Test 4: Mid-FY joiner (September), pro-rated income spans every slab ────
    //           including the top 30% band → TDS applies.
    // Joins September 15, 2024; payroll month = September 2024.
    // Months remaining (Sep → Mar inclusive) = 7
    // monthlyGross = ₹2,50,000 → 7-month gross = ₹17,50,000 → taxable = ₹16,75,000
    // slab tax = 400,000×5% + 300,000×10% + 200,000×15% + 300,000×20% + 175,000×30% = ₹1,92,500
    // + 4% cess = ₹2,00,200 → monthlyTds = ₹2,00,200 / 7 = ₹28,600.00
    @Test
    @DisplayName("Mid-FY September joiner with high pro-rated income pays TDS across every slab")
    void midFY_septemberJoiner_proRatedIncomeAcrossAllSlabs_returnsCorrectTds() {
        BigDecimal monthlyGross = new BigDecimal("250000");
        LocalDate joiningDate   = LocalDate.of(2024, 9, 15);
        LocalDate payrollMonth  = LocalDate.of(2024, 9, 1);

        BigDecimal result = taxCalculator.calculateMonthlyTds(
                monthlyGross, joiningDate, payrollMonth, TaxRegime.NEW, Optional.empty());

        assertThat(result).isEqualByComparingTo(new BigDecimal("28600.00"));
    }
}
