package com.hrms.payroll.service.impl;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.enums.EmploymentStatus;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.payroll.dto.PayableSummaryResponse;
import com.hrms.payroll.dto.SalaryStructureRequest;
import com.hrms.payroll.dto.SalaryStructureResponse;
import com.hrms.payroll.entity.PayrollComponent;
import com.hrms.payroll.entity.SalaryStructure;
import com.hrms.payroll.enums.CalculationType;
import com.hrms.payroll.mapper.SalaryStructureMapper;
import com.hrms.payroll.repository.PayrollComponentRepository;
import com.hrms.payroll.repository.SalaryStructureRepository;
import com.hrms.payroll.service.SalaryStructureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SalaryStructureServiceImpl implements SalaryStructureService {

    // Fallback percentages if no PayrollComponent record exists for that code
    private static final BigDecimal PCT_BASIC      = new BigDecimal("0.40");
    private static final BigDecimal PCT_HRA        = new BigDecimal("0.20");
    private static final BigDecimal PCT_DA         = new BigDecimal("0.10");
    private static final BigDecimal PCT_CONVEYANCE = new BigDecimal("0.10");
    private static final BigDecimal PCT_MEDICAL    = new BigDecimal("0.05");

    // PF / ESI statutory constants
    private static final BigDecimal PF_RATE        = new BigDecimal("0.12");
    private static final BigDecimal PF_BASIC_CAP   = new BigDecimal("15000");
    private static final BigDecimal PF_MAX_MONTHLY = new BigDecimal("1800");
    private static final BigDecimal ESI_EMP_RATE   = new BigDecimal("0.0075");
    private static final BigDecimal ESI_EMP_R_RATE = new BigDecimal("0.0325");
    private static final BigDecimal ESI_GROSS_LIMIT = new BigDecimal("21000");
    private static final BigDecimal PT_THRESHOLD   = new BigDecimal("10000");
    private static final BigDecimal PT_AMOUNT      = new BigDecimal("200.00");

    private final SalaryStructureRepository    salaryStructureRepository;
    private final EmployeeRepository           employeeRepository;
    private final PayrollComponentRepository   payrollComponentRepository;
    private final SalaryStructureMapper        salaryStructureMapper;

    @Override
    public SalaryStructureResponse create(SalaryStructureRequest request) {
        Employee employee = findEmployeeOrThrow(request.getEmployeeId());
        closeActiveStructures(employee.getId(), request.getEffectiveFrom());
        SalaryStructure structure = buildStructure(employee, request.getAnnualCtc(), request.getEffectiveFrom());
        structure = salaryStructureRepository.save(structure);
        log.info("Created salary structure for employee: {}", employee.getId());
        return salaryStructureMapper.toResponse(structure);
    }

    @Override
    @Transactional(readOnly = true)
    public SalaryStructureResponse getById(UUID id) {
        return salaryStructureMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalaryStructureResponse> getByEmployee(UUID employeeId) {
        findEmployeeOrThrow(employeeId);
        return salaryStructureRepository.findByEmployeeIdOrderByEffectiveFromDesc(employeeId).stream()
                .map(salaryStructureMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SalaryStructureResponse getActiveForEmployee(UUID employeeId) {
        findEmployeeOrThrow(employeeId);
        List<SalaryStructure> structures = salaryStructureRepository
                .findActiveForEmployee(employeeId, LocalDate.now());
        if (structures.isEmpty()) {
            throw new ResourceNotFoundException("SalaryStructure", "employeeId", employeeId.toString());
        }
        return salaryStructureMapper.toResponse(structures.get(0));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SalaryStructureResponse> getMyActiveSalaryStructure(UUID employeeId) {
        List<SalaryStructure> structures = salaryStructureRepository
                .findActiveForEmployee(employeeId, LocalDate.now());
        return structures.isEmpty()
                ? Optional.empty()
                : Optional.of(salaryStructureMapper.toResponse(structures.get(0)));
    }

    @Override
    @Transactional(readOnly = true)
    public PayableSummaryResponse getTotalPayableSummary() {
        List<SalaryStructure> actives = salaryStructureRepository
                .findActiveForEmployeesByStatus(EmploymentStatus.ACTIVE);
        BigDecimal totalGross = actives.stream()
                .map(s -> s.getGrossSalary() != null ? s.getGrossSalary() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCtc = actives.stream()
                .map(s -> s.getAnnualCtc() != null ? s.getAnnualCtc() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PayableSummaryResponse(actives.size(), totalGross, totalCtc);
    }

    @Override
    public SalaryStructureResponse update(UUID id, SalaryStructureRequest request) {
        SalaryStructure existing = findOrThrow(id);
        Employee employee = findEmployeeOrThrow(request.getEmployeeId());
        closeActiveStructures(employee.getId(), request.getEffectiveFrom());
        SalaryStructure revised = buildStructure(employee, request.getAnnualCtc(), request.getEffectiveFrom());
        salaryStructureRepository.delete(existing);
        revised = salaryStructureRepository.save(revised);
        log.info("Revised salary structure for employee: {}", employee.getId());
        return salaryStructureMapper.toResponse(revised);
    }

    @Override
    public void delete(UUID id) {
        SalaryStructure structure = findOrThrow(id);
        salaryStructureRepository.delete(structure);
        log.info("Deleted salary structure: {}", id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private SalaryStructure buildStructure(Employee employee, BigDecimal annualCtc, LocalDate effectiveFrom) {
        BigDecimal monthlyGross = annualCtc.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        // Load active PayrollComponents from DB so percentages/amounts are configurable
        Map<String, PayrollComponent> byCode = payrollComponentRepository
                .findByActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .collect(Collectors.toMap(PayrollComponent::getCode, c -> c));

        BigDecimal basic      = pctComponent(monthlyGross, byCode, "BASIC",       PCT_BASIC);
        BigDecimal hra        = pctComponent(monthlyGross, byCode, "HRA",         PCT_HRA);
        BigDecimal da         = pctComponent(monthlyGross, byCode, "DA",          PCT_DA);
        BigDecimal conveyance = pctComponent(monthlyGross, byCode, "CONVEYANCE",  PCT_CONVEYANCE);
        BigDecimal medical    = fixedOrPct  (monthlyGross, byCode, "MEDICAL",     PCT_MEDICAL);
        BigDecimal special    = monthlyGross
                .subtract(basic).subtract(hra).subtract(da).subtract(conveyance).subtract(medical)
                .max(BigDecimal.ZERO);

        // ── PF (statutory, always applicable) ────────────────────────────────
        BigDecimal pfBase      = basic.min(PF_BASIC_CAP);
        BigDecimal pfEmployee  = pfBase.multiply(PF_RATE).min(PF_MAX_MONTHLY).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pfEmployer  = pfEmployee; // employer matches employee contribution

        // ── ESI (only if monthly gross ≤ ₹21,000) ────────────────────────────
        boolean esiApplicable  = monthlyGross.compareTo(ESI_GROSS_LIMIT) <= 0;
        BigDecimal esiEmployee = esiApplicable
                ? monthlyGross.multiply(ESI_EMP_RATE).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal esiEmployer = esiApplicable
                ? monthlyGross.multiply(ESI_EMP_R_RATE).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // ── Professional Tax (flat ₹200 if gross > ₹10,000) ──────────────────
        BigDecimal pt = profTax(monthlyGross, byCode);

        // Net = Gross - PF(emp) - ESI(emp) - PT  (TDS is computed monthly at run time)
        BigDecimal netSalary = monthlyGross.subtract(pfEmployee).subtract(esiEmployee).subtract(pt);

        return SalaryStructure.builder()
                .employee(employee)
                .annualCtc(annualCtc)
                .ctc(annualCtc)
                .effectiveFrom(effectiveFrom)
                .effectiveTo(null)
                .grossSalary(monthlyGross)
                .basic(basic)
                .hra(hra)
                .da(da)
                .conveyance(conveyance)
                .medicalAllowance(medical)
                .specialAllowance(special)
                .transportAllowance(BigDecimal.ZERO)  // legacy column — kept for compat
                .lta(BigDecimal.ZERO)                 // legacy column — kept for compat
                .pfApplicable(true)
                .pfEmployee(pfEmployee)
                .pfEmployer(pfEmployer)
                .esiApplicable(esiApplicable)
                .esiEmployee(esiEmployee)
                .esiEmployer(esiEmployer)
                .professionalTax(pt)
                .netSalary(netSalary)
                .active(true)
                .build();
    }

    /** Returns the PERCENTAGE-of-gross amount for a component, or falls back to {@code defaultPct}. */
    private static BigDecimal pctComponent(BigDecimal base,
                                           Map<String, PayrollComponent> byCode,
                                           String code,
                                           BigDecimal defaultPct) {
        PayrollComponent c = byCode.get(code);
        if (c == null || c.getValue() == null || c.getCalculationType() != CalculationType.PERCENTAGE) {
            return pct(base, defaultPct);
        }
        // DB stores value as 40.00 (meaning 40%), divide by 100 to get ratio
        return base.multiply(c.getValue().divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Returns FIXED amount from DB, falls back to PERCENTAGE of base if FIXED not configured. */
    private static BigDecimal fixedOrPct(BigDecimal base,
                                         Map<String, PayrollComponent> byCode,
                                         String code,
                                         BigDecimal defaultPct) {
        PayrollComponent c = byCode.get(code);
        if (c == null || c.getValue() == null) return pct(base, defaultPct);
        if (c.getCalculationType() == CalculationType.FIXED) {
            return c.getValue().setScale(2, RoundingMode.HALF_UP);
        }
        return base.multiply(c.getValue().divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Professional Tax: ₹200/month if gross > ₹10,000 (reads from PT component or uses default). */
    private static BigDecimal profTax(BigDecimal monthlyGross, Map<String, PayrollComponent> byCode) {
        if (monthlyGross.compareTo(PT_THRESHOLD) <= 0) return BigDecimal.ZERO;
        PayrollComponent pt = byCode.get("PT");
        if (pt != null && pt.getValue() != null && pt.getCalculationType() == CalculationType.FIXED) {
            return pt.getValue().setScale(2, RoundingMode.HALF_UP);
        }
        return PT_AMOUNT;
    }

    private void closeActiveStructures(UUID employeeId, LocalDate newEffectiveFrom) {
        List<SalaryStructure> actives = salaryStructureRepository
                .findActiveForEmployee(employeeId, newEffectiveFrom);
        for (SalaryStructure s : actives) {
            s.setActive(false);
            s.setEffectiveTo(newEffectiveFrom.minusDays(1));
            salaryStructureRepository.save(s);
        }
    }

    private static BigDecimal pct(BigDecimal base, BigDecimal rate) {
        return base.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    private SalaryStructure findOrThrow(UUID id) {
        return salaryStructureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalaryStructure", "id", id.toString()));
    }

    private Employee findEmployeeOrThrow(UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId.toString()));
    }
}
