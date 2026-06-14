package com.hrms.payroll.util;

import com.hrms.payroll.entity.TdsDeclaration;
import com.hrms.payroll.enums.TaxRegime;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Monthly TDS calculator supporting both new-regime and old-regime slabs (FY 2024-25).
 *
 * New regime slabs (no exemptions except standard deduction ₹75,000):
 *   0 – 3,00,000      : 0%
 *   3,00,001 – 7,00,000  : 5%
 *   7,00,001 – 10,00,000 : 10%
 *   10,00,001 – 12,00,000: 15%
 *   12,00,001 – 15,00,000: 20%
 *   Above 15,00,000    : 30%
 *   87A rebate: tax = 0 if taxable income ≤ ₹7,00,000
 *
 * Old regime slabs (standard deduction ₹50,000 + investment exemptions):
 *   0 – 2,50,000       : 0%
 *   2,50,001 – 5,00,000 : 5%
 *   5,00,001 – 10,00,000: 20%
 *   Above 10,00,000    : 30%
 *   87A rebate: tax = 0 if taxable income ≤ ₹5,00,000
 *
 * Health & Education Cess: 4% on computed income tax.
 */
@Component
public class TaxCalculator {

    // New-regime slab boundaries and rates
    private static final BigDecimal NEW_SLAB_1 = new BigDecimal("300000");
    private static final BigDecimal NEW_SLAB_2 = new BigDecimal("700000");
    private static final BigDecimal NEW_SLAB_3 = new BigDecimal("1000000");
    private static final BigDecimal NEW_SLAB_4 = new BigDecimal("1200000");
    private static final BigDecimal NEW_SLAB_5 = new BigDecimal("1500000");

    private static final BigDecimal NEW_STD_DEDUCTION = new BigDecimal("75000");
    private static final BigDecimal NEW_87A_LIMIT     = new BigDecimal("700000");

    // Old-regime slab boundaries and rates
    private static final BigDecimal OLD_SLAB_1 = new BigDecimal("250000");
    private static final BigDecimal OLD_SLAB_2 = new BigDecimal("500000");
    private static final BigDecimal OLD_SLAB_3 = new BigDecimal("1000000");

    private static final BigDecimal OLD_STD_DEDUCTION = new BigDecimal("50000");
    private static final BigDecimal OLD_87A_LIMIT     = new BigDecimal("500000");

    // 80C cap
    private static final BigDecimal CAP_80C = new BigDecimal("150000");
    // 80D cap (self; simplified — no separate senior-citizen cap)
    private static final BigDecimal CAP_80D = new BigDecimal("25000");

    private static final BigDecimal CESS_RATE = new BigDecimal("0.04");

    /**
     * Calculates the monthly TDS amount for an employee.
     *
     * @param monthlyGross  full-month gross salary (from salary structure)
     * @param joiningDate   employee joining date (for mid-FY pro-ration)
     * @param payrollMonth  any date within the payroll month
     * @param regime        NEW or OLD tax regime (from TDS declaration; defaults to NEW)
     * @param declaration   employee's TDS/investment declaration for the FY (may be empty)
     * @return monthly TDS, rounded to 2 decimal places HALF_UP
     */
    public BigDecimal calculateMonthlyTds(BigDecimal monthlyGross,
                                          LocalDate joiningDate,
                                          LocalDate payrollMonth,
                                          TaxRegime regime,
                                          Optional<TdsDeclaration> declaration) {

        LocalDate fyStart = FinancialYearUtil.getIndianFyStart(payrollMonth);
        LocalDate fyEnd   = FinancialYearUtil.getIndianFyEnd(payrollMonth);

        // Pro-rate from whichever is later: FY start or employee's joining month
        LocalDate joiningMonthStart   = joiningDate.withDayOfMonth(1);
        LocalDate effectiveStartMonth = joiningMonthStart.isAfter(fyStart) ? joiningMonthStart : fyStart;

        int monthsRemaining = FinancialYearUtil.monthsRemainingInFY(effectiveStartMonth, fyEnd);
        if (monthsRemaining <= 0) return BigDecimal.ZERO;

        BigDecimal annualGross = monthlyGross.multiply(BigDecimal.valueOf(monthsRemaining));

        BigDecimal annualTax;
        if (regime == TaxRegime.OLD) {
            annualTax = computeOldRegimeTax(annualGross, declaration);
        } else {
            annualTax = computeNewRegimeTax(annualGross);
        }

        // Add 4% Health & Education Cess
        annualTax = annualTax.multiply(BigDecimal.ONE.add(CESS_RATE)).setScale(2, RoundingMode.HALF_UP);

        return annualTax.divide(BigDecimal.valueOf(monthsRemaining), 2, RoundingMode.HALF_UP);
    }

    // ── New regime ─────────────────────────────────────────────────────────────

    private BigDecimal computeNewRegimeTax(BigDecimal annualGross) {
        BigDecimal taxable = annualGross.subtract(NEW_STD_DEDUCTION).max(BigDecimal.ZERO);

        // 87A rebate: no tax if taxable income ≤ ₹7L
        if (taxable.compareTo(NEW_87A_LIMIT) <= 0) return BigDecimal.ZERO;

        return slabTax(taxable,
                new long[]{0, 300_000, 700_000, 1_000_000, 1_200_000, 1_500_000},
                new int[] {0,       5,        10,         15,         20,         30});
    }

    // ── Old regime ─────────────────────────────────────────────────────────────

    private BigDecimal computeOldRegimeTax(BigDecimal annualGross, Optional<TdsDeclaration> declaration) {
        BigDecimal taxable = annualGross.subtract(OLD_STD_DEDUCTION).max(BigDecimal.ZERO);

        if (declaration.isPresent()) {
            TdsDeclaration d = declaration.get();
            taxable = taxable.subtract(nvl(d.getHraExemption()));
            taxable = taxable.subtract(nvl(d.getSection80c()).min(CAP_80C));
            taxable = taxable.subtract(nvl(d.getSection80d()).min(CAP_80D));
            taxable = taxable.subtract(nvl(d.getSection80g()));
            taxable = taxable.subtract(nvl(d.getOtherDeductions()));
            taxable = taxable.max(BigDecimal.ZERO);
        }

        // 87A rebate: no tax if taxable income ≤ ₹5L
        if (taxable.compareTo(OLD_87A_LIMIT) <= 0) return BigDecimal.ZERO;

        return slabTax(taxable,
                new long[]{0, 250_000, 500_000, 1_000_000},
                new int[] {0,       5,        20,         30});
    }

    // ── Slab engine ────────────────────────────────────────────────────────────

    /**
     * Computes progressive slab tax.
     *
     * @param taxable  taxable income (after deductions)
     * @param slabs    lower bound of each slab in ascending order (first must be 0)
     * @param rates    tax rate (%) for each slab band
     */
    private static BigDecimal slabTax(BigDecimal taxable, long[] slabs, int[] rates) {
        BigDecimal tax = BigDecimal.ZERO;
        for (int i = 0; i < slabs.length; i++) {
            BigDecimal slabStart = BigDecimal.valueOf(slabs[i]);
            BigDecimal slabEnd   = (i + 1 < slabs.length)
                    ? BigDecimal.valueOf(slabs[i + 1])
                    : null; // last slab = unbounded

            if (taxable.compareTo(slabStart) <= 0) break;

            BigDecimal taxableInBand = (slabEnd == null)
                    ? taxable.subtract(slabStart)
                    : taxable.min(slabEnd).subtract(slabStart).max(BigDecimal.ZERO);

            tax = tax.add(taxableInBand.multiply(BigDecimal.valueOf(rates[i]))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        }
        return tax;
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
