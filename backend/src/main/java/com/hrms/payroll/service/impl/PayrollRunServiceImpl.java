package com.hrms.payroll.service.impl;

import com.hrms.attendance.service.AttendanceDayLedgerService;
import com.hrms.common.dto.PageableResponse;
import com.hrms.common.exception.BusinessException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.payroll.dto.InitiatePayrollRunRequest;
import com.hrms.payroll.dto.PayrollRunResponse;
import com.hrms.payroll.dto.PayslipResponse;
import com.hrms.payroll.entity.Payslip;
import com.hrms.payroll.entity.PayslipComponent;
import com.hrms.payroll.entity.PayrollRun;
import com.hrms.payroll.entity.SalaryStructure;
import com.hrms.payroll.entity.SalaryStructureComponent;
import com.hrms.payroll.entity.TdsDeclaration;
import com.hrms.payroll.enums.PayrollRunStatus;
import com.hrms.payroll.enums.TaxRegime;
import com.hrms.payroll.mapper.PayrollRunMapper;
import com.hrms.payroll.mapper.PayslipComponentMapper;
import com.hrms.payroll.mapper.PayslipMapper;
import com.hrms.payroll.repository.PayrollRunRepository;
import com.hrms.payroll.repository.PayslipComponentRepository;
import com.hrms.payroll.repository.PayslipRepository;
import com.hrms.payroll.repository.SalaryStructureComponentRepository;
import com.hrms.payroll.repository.SalaryStructureRepository;
import com.hrms.payroll.repository.TdsDeclarationRepository;
import com.hrms.payroll.service.PayrollRunService;
import com.hrms.payroll.util.FinancialYearUtil;
import com.hrms.payroll.util.PayslipPdfService;
import com.hrms.payroll.util.TaxCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PayrollRunServiceImpl implements PayrollRunService {

    private static final BigDecimal PF_RATE         = new BigDecimal("0.12");
    private static final BigDecimal PF_BASIC_CAP    = new BigDecimal("15000");
    private static final BigDecimal PF_MAX_MONTHLY  = new BigDecimal("1800");
    private static final BigDecimal ESI_EMP_RATE    = new BigDecimal("0.0075");
    private static final BigDecimal ESI_GROSS_LIMIT = new BigDecimal("21000");
    private static final BigDecimal PT_THRESHOLD    = new BigDecimal("10000");
    private static final BigDecimal PT_AMOUNT       = new BigDecimal("200.00");

    private final PayrollRunRepository                payrollRunRepository;
    private final PayslipRepository                   payslipRepository;
    private final PayslipComponentRepository          payslipComponentRepository;
    private final SalaryStructureRepository           salaryStructureRepository;
    private final SalaryStructureComponentRepository  salaryStructureComponentRepository;
    private final EmployeeRepository                  employeeRepository;
    private final TdsDeclarationRepository            tdsDeclarationRepository;
    private final PayrollRunMapper                    payrollRunMapper;
    private final PayslipMapper                       payslipMapper;
    private final PayslipComponentMapper              payslipComponentMapper;
    private final TaxCalculator                       taxCalculator;
    private final PayslipPdfService                   payslipPdfService;
    private final AttendanceDayLedgerService          attendanceDayLedgerService;

    @Override
    public PayrollRunResponse initiate(InitiatePayrollRunRequest request, UUID processedBy) {
        if (payrollRunRepository.findByYearAndMonth(request.getYear(), request.getMonth()).isPresent()) {
            throw new BusinessException(
                    "Payroll run for " + request.getYear() + "/" + request.getMonth() + " already exists");
        }
        PayrollRun run = PayrollRun.builder()
                .year(request.getYear())
                .month(request.getMonth())
                .runDate(Instant.now())
                .status(PayrollRunStatus.DRAFT)
                .totalEmployees(0)
                .totalGross(BigDecimal.ZERO)
                .totalDeductions(BigDecimal.ZERO)
                .totalNet(BigDecimal.ZERO)
                .processedBy(processedBy)
                .remarks(request.getRemarks())
                .build();
        run = payrollRunRepository.save(run);
        log.info("Initiated payroll run for {}/{}", request.getYear(), request.getMonth());
        return payrollRunMapper.toResponse(run);
    }

    @Override
    public PayrollRunResponse process(UUID runId) {
        PayrollRun run = findOrThrow(runId);
        if (run.getStatus() != PayrollRunStatus.DRAFT) {
            throw new BusinessException("Payroll run can only be processed from DRAFT status");
        }

        LocalDate runMonth = LocalDate.of(run.getYear(), run.getMonth(), 1);
        List<Employee> eligibleEmployees = employeeRepository.findAll().stream()
                .filter(e -> isEmployedDuringMonth(e, runMonth))
                .toList();

        BigDecimal totalGross      = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal totalNet        = BigDecimal.ZERO;
        int processedCount = 0;

        for (Employee employee : eligibleEmployees) {
            Payslip payslip = processEmployeeForRun(employee, run);
            if (payslip == null) continue;
            totalGross      = totalGross.add(nvl(payslip.getGrossSalary()));
            totalDeductions = totalDeductions.add(nvl(payslip.getTotalDeductions()));
            totalNet        = totalNet.add(nvl(payslip.getNetSalary()));
            processedCount++;
        }

        run.setTotalEmployees(processedCount);
        run.setTotalGross(totalGross);
        run.setTotalDeductions(totalDeductions);
        run.setTotalNet(totalNet);
        run.setStatus(PayrollRunStatus.PROCESSED);
        run.setProcessedAt(Instant.now());
        run = payrollRunRepository.save(run);

        log.info("Processed payroll run {} – {} employees", runId, processedCount);
        return payrollRunMapper.toResponse(run);
    }

    @Override
    public PayslipResponse generatePayslipForEmployee(UUID employeeId, int month, int year, UUID requestedBy) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId.toString()));

        if (payslipRepository.findByEmployeeIdAndYearAndMonth(employeeId, year, month).isPresent()) {
            throw new BusinessException("Payslip already generated for " + employee.getFirstName()
                    + " " + employee.getLastName() + " for " + monthLabel(month, year) + ".");
        }

        // Find or create a DRAFT run for this period
        PayrollRun run = payrollRunRepository.findByYearAndMonth(year, month)
                .orElseGet(() -> payrollRunRepository.save(PayrollRun.builder()
                        .year(year).month(month)
                        .runDate(Instant.now())
                        .status(PayrollRunStatus.DRAFT)
                        .totalEmployees(0)
                        .totalGross(BigDecimal.ZERO)
                        .totalDeductions(BigDecimal.ZERO)
                        .totalNet(BigDecimal.ZERO)
                        .processedBy(requestedBy)
                        .build()));

        if (run.getStatus() != PayrollRunStatus.DRAFT) {
            throw new BusinessException(
                    "Cannot generate individual payslip — run for " + month + "/" + year
                    + " is already " + run.getStatus());
        }

        Payslip payslip = processEmployeeForRun(employee, run);
        if (payslip == null) {
            throw new BusinessException(
                    "No active salary structure for employee " + employeeId + " in " + month + "/" + year);
        }

        // Auto-publish so the employee can see it immediately
        try {
            String path = payslipPdfService.generateAndSave(payslip);
            payslip.setPdfUrl(path);
        } catch (Exception ex) {
            log.error("PDF generation failed for payslip {}: {}", payslip.getId(), ex.getMessage());
        }
        payslip.setPublished(true);
        payslip.setPublishedAt(Instant.now());
        payslip = payslipRepository.save(payslip);

        // Recalculate run aggregates from all payslips in this run
        List<Payslip> all = payslipRepository.findByPayrollRunId(run.getId());
        run.setTotalEmployees((int) all.stream().map(p -> p.getEmployee().getId()).distinct().count());
        run.setTotalGross(all.stream().map(p -> nvl(p.getGrossSalary())).reduce(BigDecimal.ZERO, BigDecimal::add));
        run.setTotalDeductions(all.stream().map(p -> nvl(p.getTotalDeductions())).reduce(BigDecimal.ZERO, BigDecimal::add));
        run.setTotalNet(all.stream().map(p -> nvl(p.getNetSalary())).reduce(BigDecimal.ZERO, BigDecimal::add));
        payrollRunRepository.save(run);

        log.info("Generated and published payslip for employee {} in run {}", employeeId, run.getId());
        return toResponseWithComponents(payslip);
    }

    @Override
    public PayrollRunResponse approve(UUID runId, UUID approvedBy) {
        PayrollRun run = findOrThrow(runId);
        if (run.getStatus() != PayrollRunStatus.PROCESSED) {
            throw new BusinessException("Payroll run must be in PROCESSED status to approve");
        }
        run.setStatus(PayrollRunStatus.APPROVED);
        run.setApprovedBy(approvedBy);
        run.setApprovedAt(Instant.now());
        run = payrollRunRepository.save(run);

        List<Payslip> payslips = payslipRepository.findByPayrollRunId(runId);
        for (Payslip payslip : payslips) {
            try {
                String path = payslipPdfService.generateAndSave(payslip);
                payslip.setPdfUrl(path);
            } catch (Exception ex) {
                log.error("PDF generation failed for payslip {}: {}", payslip.getId(), ex.getMessage());
            }
            payslip.setPublished(true);
            payslip.setPublishedAt(Instant.now());
            payslipRepository.save(payslip);
        }

        log.info("Approved payroll run: {} – {} PDFs generated", runId, payslips.size());
        return payrollRunMapper.toResponse(run);
    }

    @Override
    public PayrollRunResponse reject(UUID runId) {
        PayrollRun run = findOrThrow(runId);
        if (run.getStatus() != PayrollRunStatus.PROCESSED) {
            throw new BusinessException("Only PROCESSED payroll runs can be rejected");
        }
        // Delete generated payslips so they are re-created on re-process
        payslipRepository.deleteAll(payslipRepository.findByPayrollRunId(runId));

        run.setStatus(PayrollRunStatus.DRAFT);
        run.setTotalEmployees(0);
        run.setTotalGross(BigDecimal.ZERO);
        run.setTotalDeductions(BigDecimal.ZERO);
        run.setTotalNet(BigDecimal.ZERO);
        run.setProcessedAt(null);
        run = payrollRunRepository.save(run);
        log.info("Rejected payroll run {} – moved back to DRAFT", runId);
        return payrollRunMapper.toResponse(run);
    }

    @Override
    public PayrollRunResponse disburse(UUID runId, UUID disbursedBy) {
        PayrollRun run = findOrThrow(runId);
        if (run.getStatus() != PayrollRunStatus.APPROVED) {
            throw new BusinessException("Only APPROVED payroll runs can be disbursed");
        }
        run.setStatus(PayrollRunStatus.DISBURSED);
        run.setDisbursedBy(disbursedBy);
        run.setDisbursedAt(Instant.now());
        run = payrollRunRepository.save(run);
        log.info("Disbursed payroll run: {}", runId);
        return payrollRunMapper.toResponse(run);
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollRunResponse getById(UUID id) {
        return payrollRunMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageableResponse<PayrollRunResponse> getAll(Pageable pageable) {
        Page<PayrollRunResponse> page = payrollRunRepository.findAll(pageable)
                .map(payrollRunMapper::toResponse);
        return PageableResponse.of(page);
    }

    // ── Per-employee payslip computation ──────────────────────────────────────

    /**
     * Computes and upserts the payslip for one employee within a payroll run.
     * Returns the saved Payslip, or null if the employee has no active salary structure.
     */
    private Payslip processEmployeeForRun(Employee employee, PayrollRun run) {
        LocalDate runMonth = LocalDate.of(run.getYear(), run.getMonth(), 1);
        LocalDate monthEnd = runMonth.with(TemporalAdjusters.lastDayOfMonth());
        String    financialYear = FinancialYearUtil.toFyString(runMonth);

        List<SalaryStructure> active = salaryStructureRepository
                .findActiveForEmployee(employee.getId(), runMonth);
        if (active.isEmpty()) return null;

        SalaryStructure structure = active.get(0);
        BigDecimal monthlyGross = nvl(structure.getGrossSalary());

        // ── Day-count: the ratio's divisor is ALWAYS the full calendar month —
        //    never shrunk for a joiner/leaver, or proration collapses to a no-op. ──
        int totalDaysInMonth = runMonth.lengthOfMonth();

        // ── Employed window within this month: starts late if hired mid-month,
        //    ends early if resigned/relieved mid-month. Days outside this window are
        //    simply unpaid (they weren't employed then) — that's what drives the
        //    proration. Days *inside* the window are further reduced by LOP
        //    (absences / unpaid-leave halves) from the attendance day ledger. ──
        LocalDate joinDate       = employee.getJoiningDate();
        LocalDate resignDate     = employee.getResignationDate();
        LocalDate rangeStart     = (joinDate != null && joinDate.isAfter(runMonth)) ? joinDate : runMonth;
        LocalDate rangeEnd       = (resignDate != null && resignDate.isBefore(monthEnd)) ? resignDate : monthEnd;
        int employedDays = rangeEnd.isBefore(rangeStart)
                ? 0
                : (int) (rangeEnd.toEpochDay() - rangeStart.toEpochDay() + 1);

        BigDecimal lopDays = employedDays == 0
                ? BigDecimal.ZERO
                : attendanceDayLedgerService
                        .getLopDays(employee.getId(), rangeStart, rangeEnd)
                        .min(BigDecimal.valueOf(employedDays));

        BigDecimal paidDays = BigDecimal.valueOf(employedDays).subtract(lopDays).max(BigDecimal.ZERO);
        BigDecimal ratio = paidDays
                .divide(BigDecimal.valueOf(totalDaysInMonth), 6, RoundingMode.HALF_UP);

        // ── Pro-rate all earning components (open-ended, whatever's on the structure) ──
        BigDecimal grossPay = monthlyGross.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
        List<SalaryStructureComponent> structureComponents = salaryStructureComponentRepository
                .findBySalaryStructureId(structure.getId());

        List<ProratedComponent> proratedComponents = structureComponents.stream()
                .map(sc -> new ProratedComponent(sc.getPayrollComponent(), proRate(nvl(sc.getComputedAmount()), ratio)))
                .toList();

        BigDecimal basic = proratedComponents.stream()
                .filter(pc -> "BASIC".equals(pc.component().getCode()))
                .findFirst()
                .map(ProratedComponent::amount)
                .orElse(BigDecimal.ZERO);

        // ── LOP amount ────────────────────────────────────────────────────────
        BigDecimal lopAmount = BigDecimal.ZERO;
        if (lopDays.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal dailyRate = monthlyGross
                    .divide(BigDecimal.valueOf(totalDaysInMonth), 2, RoundingMode.HALF_UP);
            lopAmount = dailyRate.multiply(lopDays).setScale(2, RoundingMode.HALF_UP);
        }

        // ── PF (12% of pro-rated basic, capped at ₹1,800) ────────────────────
        BigDecimal pfAmt = BigDecimal.ZERO;
        if (structure.isPfApplicable()) {
            pfAmt = basic.min(PF_BASIC_CAP).multiply(PF_RATE)
                    .min(PF_MAX_MONTHLY.multiply(ratio))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // ── ESI (0.75% of pro-rated gross if applicable and gross ≤ ₹21k) ─────
        BigDecimal esiAmt = BigDecimal.ZERO;
        if (structure.isEsiApplicable() && grossPay.compareTo(ESI_GROSS_LIMIT) <= 0) {
            esiAmt = grossPay.multiply(ESI_EMP_RATE).setScale(2, RoundingMode.HALF_UP);
        }

        // ── Professional Tax (pro-rated; applies if full-month gross > ₹10k) ───
        BigDecimal ptAmt = BigDecimal.ZERO;
        if (monthlyGross.compareTo(PT_THRESHOLD) > 0) {
            BigDecimal fullPt = nvl(structure.getProfessionalTax()).compareTo(BigDecimal.ZERO) > 0
                    ? structure.getProfessionalTax()
                    : PT_AMOUNT;
            ptAmt = proRate(fullPt, ratio);
        }

        // ── TDS (projection-based; lookup employee's FY declaration) ──────────
        LocalDate taxJoinDate = joinDate != null ? joinDate : LocalDate.of(2000, 1, 1);
        Optional<TdsDeclaration> declaration = tdsDeclarationRepository
                .findByEmployeeIdAndFinancialYear(employee.getId(), financialYear);
        TaxRegime regime = declaration.map(TdsDeclaration::getRegimeType).orElse(TaxRegime.NEW);

        BigDecimal monthlyTds = taxCalculator.calculateMonthlyTds(
                monthlyGross, taxJoinDate, runMonth, regime, declaration);
        BigDecimal proratedTds = monthlyTds.multiply(ratio).setScale(2, RoundingMode.HALF_UP);

        // ── Totals ────────────────────────────────────────────────────────────
        BigDecimal totalDed = proratedTds.add(pfAmt).add(esiAmt).add(ptAmt);
        BigDecimal netPay   = grossPay.subtract(totalDed).max(BigDecimal.ZERO);

        // ── Upsert payslip ────────────────────────────────────────────────────
        Optional<Payslip> existing = payslipRepository
                .findByEmployeeIdAndYearAndMonth(employee.getId(), run.getYear(), run.getMonth());
        Payslip payslip = existing.orElse(new Payslip());
        payslip.setPayrollRun(run);
        payslip.setEmployee(employee);
        payslip.setYear(run.getYear());
        payslip.setMonth(run.getMonth());

        payslip.setGrossSalary(grossPay);

        payslip.setPfDeduction(pfAmt);
        payslip.setEsiDeduction(esiAmt);
        payslip.setProfessionalTax(ptAmt);
        payslip.setTds(proratedTds);
        payslip.setLoanDeduction(BigDecimal.ZERO);
        payslip.setAdvanceDeduction(BigDecimal.ZERO);
        payslip.setOtherDeductions(BigDecimal.ZERO);
        payslip.setTotalDeductions(totalDed);

        payslip.setLopDays(lopDays);
        payslip.setLopAmount(lopAmount);
        payslip.setNetSalary(netPay);
        payslip.setWorkingDays(totalDaysInMonth);
        payslip.setPaidDays(paidDays);
        payslip.setPublished(false);

        payslip = payslipRepository.save(payslip);

        payslipComponentRepository.deleteByPayslipId(payslip.getId());
        Payslip savedPayslip = payslip;
        List<PayslipComponent> componentRows = proratedComponents.stream()
                .map(pc -> PayslipComponent.builder()
                        .payslip(savedPayslip)
                        .payrollComponent(pc.component())
                        .amount(pc.amount())
                        .build())
                .toList();
        payslipComponentRepository.saveAll(componentRows);

        return payslip;
    }

    private record ProratedComponent(com.hrms.payroll.entity.PayrollComponent component, BigDecimal amount) {}

    private PayslipResponse toResponseWithComponents(Payslip payslip) {
        PayslipResponse response = payslipMapper.toResponse(payslip);
        response.setComponents(payslipComponentMapper.toResponseList(
                payslipComponentRepository.findByPayslipId(payslip.getId())));
        return response;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PayrollRun findOrThrow(UUID id) {
        return payrollRunRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollRun", "id", id.toString()));
    }

    private static BigDecimal proRate(BigDecimal amount, BigDecimal ratio) {
        return amount.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static String monthLabel(int month, int year) {
        return java.time.Month.of(month).getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
                + " " + year;
    }

    /**
     * ACTIVE employees are always included. INACTIVE employees (soft-deleted/terminated)
     * are still included for the month they actually left in (or a still-future exit
     * date) so their final, prorated payslip gets generated — only employees who already
     * left in a prior month are excluded.
     */
    private static boolean isEmployedDuringMonth(Employee employee, LocalDate runMonth) {
        return switch (employee.getEmploymentStatus()) {
            case ACTIVE -> true;
            case INACTIVE -> employee.getResignationDate() == null
                    || !employee.getResignationDate().isBefore(runMonth);
        };
    }
}
