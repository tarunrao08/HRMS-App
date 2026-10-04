package com.hrms.payroll.service.impl;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.exception.ValidationException;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.enums.EmploymentStatus;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.payroll.dto.PayableSummaryResponse;
import com.hrms.payroll.dto.SalaryStructureComponentInput;
import com.hrms.payroll.dto.SalaryStructureRequest;
import com.hrms.payroll.dto.SalaryStructureResponse;
import com.hrms.payroll.entity.PayrollComponent;
import com.hrms.payroll.entity.SalaryStructure;
import com.hrms.payroll.entity.SalaryStructureComponent;
import com.hrms.payroll.enums.CalculationType;
import com.hrms.payroll.enums.ComponentType;
import com.hrms.payroll.mapper.SalaryStructureComponentMapper;
import com.hrms.payroll.mapper.SalaryStructureMapper;
import com.hrms.payroll.repository.PayrollComponentRepository;
import com.hrms.payroll.repository.SalaryStructureComponentRepository;
import com.hrms.payroll.repository.SalaryStructureRepository;
import com.hrms.payroll.service.SalaryStructureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
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

    // Rounding slack allowed when reconciling non-FORMULA earning components against gross
    // (each component rounds independently to 2 decimals, so small drift is expected).
    private static final BigDecimal RECONCILE_TOLERANCE = new BigDecimal("0.10");

    // PF / ESI / PT statutory constants — unchanged, compliance-mandated formulas, not
    // part of the open-ended earning catalog.
    private static final BigDecimal PF_RATE        = new BigDecimal("0.12");
    private static final BigDecimal PF_BASIC_CAP   = new BigDecimal("15000");
    private static final BigDecimal PF_MAX_MONTHLY = new BigDecimal("1800");
    private static final BigDecimal ESI_EMP_RATE   = new BigDecimal("0.0075");
    private static final BigDecimal ESI_EMP_R_RATE = new BigDecimal("0.0325");
    private static final BigDecimal ESI_GROSS_LIMIT = new BigDecimal("21000");
    private static final BigDecimal PT_THRESHOLD   = new BigDecimal("10000");
    private static final BigDecimal PT_AMOUNT      = new BigDecimal("200.00");

    private final SalaryStructureRepository          salaryStructureRepository;
    private final SalaryStructureComponentRepository salaryStructureComponentRepository;
    private final EmployeeRepository                 employeeRepository;
    private final PayrollComponentRepository         payrollComponentRepository;
    private final SalaryStructureMapper              salaryStructureMapper;
    private final SalaryStructureComponentMapper     salaryStructureComponentMapper;

    @Override
    public SalaryStructureResponse create(SalaryStructureRequest request) {
        Employee employee = findEmployeeOrThrow(request.getEmployeeId());
        closeActiveStructures(employee.getId(), request.getEffectiveFrom());
        SalaryStructure structure = buildAndSaveStructure(
                employee, request.getAnnualCtc(), request.getEffectiveFrom(), request.getComponents());
        log.info("Created salary structure for employee: {}", employee.getId());
        return toResponseWithComponents(structure);
    }

    @Override
    @Transactional(readOnly = true)
    public SalaryStructureResponse getById(UUID id) {
        return toResponseWithComponents(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalaryStructureResponse> getByEmployee(UUID employeeId) {
        findEmployeeOrThrow(employeeId);
        return salaryStructureRepository.findByEmployeeIdOrderByEffectiveFromDesc(employeeId).stream()
                .map(this::toResponseWithComponents)
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
        return toResponseWithComponents(structures.get(0));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SalaryStructureResponse> getMyActiveSalaryStructure(UUID employeeId) {
        List<SalaryStructure> structures = salaryStructureRepository
                .findActiveForEmployee(employeeId, LocalDate.now());
        return structures.isEmpty()
                ? Optional.empty()
                : Optional.of(toResponseWithComponents(structures.get(0)));
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
        salaryStructureRepository.delete(existing); // cascades to salary_structure_components
        SalaryStructure revised = buildAndSaveStructure(
                employee, request.getAnnualCtc(), request.getEffectiveFrom(), request.getComponents());
        log.info("Revised salary structure for employee: {}", employee.getId());
        return toResponseWithComponents(revised);
    }

    @Override
    public void delete(UUID id) {
        SalaryStructure structure = findOrThrow(id);
        salaryStructureRepository.delete(structure);
        log.info("Deleted salary structure: {}", id);
    }

    // ── Component resolution ────────────────────────────────────────────────

    private record ResolvedComponent(PayrollComponent component, CalculationType calculationType, BigDecimal value) {}

    private SalaryStructure buildAndSaveStructure(Employee employee, BigDecimal annualCtc, LocalDate effectiveFrom,
                                                   List<SalaryStructureComponentInput> overrides) {
        BigDecimal monthlyGross = annualCtc.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        List<PayrollComponent> catalog = payrollComponentRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .filter(c -> c.getComponentType() == ComponentType.EARNING)
                .toList();
        if (catalog.isEmpty()) {
            throw new ValidationException("No active earning components are configured in the payroll catalog");
        }

        Map<UUID, SalaryStructureComponentInput> overrideByComponentId = (overrides == null ? List.<SalaryStructureComponentInput>of() : overrides)
                .stream()
                .collect(Collectors.toMap(SalaryStructureComponentInput::getPayrollComponentId, o -> o, (a, b) -> a));

        List<ResolvedComponent> resolved = new ArrayList<>();
        for (PayrollComponent pc : catalog) {
            SalaryStructureComponentInput override = overrideByComponentId.get(pc.getId());
            CalculationType calcType = (override != null && override.getCalculationType() != null)
                    ? override.getCalculationType() : pc.getCalculationType();
            BigDecimal value = (override != null && override.getValue() != null)
                    ? override.getValue() : pc.getValue();
            resolved.add(new ResolvedComponent(pc, calcType, value));
        }

        validateChainingAndResiduals(resolved);

        Map<UUID, BigDecimal> amountByComponentId = new HashMap<>();
        ResolvedComponent formulaComponent = null;

        // Pass 1: FIXED and gross-relative PERCENTAGE (percentageOfComponent == null)
        for (ResolvedComponent r : resolved) {
            switch (r.calculationType()) {
                case FIXED -> amountByComponentId.put(r.component().getId(),
                        nvl(r.value()).setScale(2, RoundingMode.HALF_UP));
                case PERCENTAGE -> {
                    if (r.component().getPercentageOfComponent() == null) {
                        amountByComponentId.put(r.component().getId(), pctOf(monthlyGross, r.value(), r.component()));
                    }
                }
                case FORMULA -> formulaComponent = r; // resolved last, after everything else
            }
        }

        // Pass 2: component-relative PERCENTAGE (validated above to reference an already-resolved, non-chained component)
        for (ResolvedComponent r : resolved) {
            if (r.calculationType() == CalculationType.PERCENTAGE && r.component().getPercentageOfComponent() != null) {
                UUID refId = r.component().getPercentageOfComponent().getId();
                BigDecimal refAmount = amountByComponentId.get(refId);
                if (refAmount == null) {
                    throw new ValidationException("Component '" + r.component().getName()
                            + "' is a percentage of '" + r.component().getPercentageOfComponent().getName()
                            + "', which isn't an active earning component");
                }
                amountByComponentId.put(r.component().getId(), pctOf(refAmount, r.value(), r.component()));
            }
        }

        // Residual / reconciliation
        if (formulaComponent != null) {
            BigDecimal sumOthers = amountByComponentId.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal residual = monthlyGross.subtract(sumOthers).max(BigDecimal.ZERO);
            amountByComponentId.put(formulaComponent.component().getId(), residual.setScale(2, RoundingMode.HALF_UP));
        } else {
            BigDecimal sum = amountByComponentId.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            if (sum.subtract(monthlyGross).abs().compareTo(RECONCILE_TOLERANCE) > 0) {
                throw new ValidationException("Configured earning components sum to " + sum
                        + " but monthly gross is " + monthlyGross
                        + ". Adjust the percentages/fixed amounts to reconcile, or mark one component as FORMULA"
                        + " to automatically absorb the remainder.");
            }
        }

        BigDecimal basicAmount = catalog.stream()
                .filter(pc -> "BASIC".equals(pc.getCode()))
                .findFirst()
                .map(pc -> amountByComponentId.getOrDefault(pc.getId(), BigDecimal.ZERO))
                .orElseThrow(() -> new ValidationException(
                        "No active 'BASIC' earning component is configured — required for PF/ESI computation"));

        // ── PF (statutory, always applicable) ────────────────────────────────
        BigDecimal pfBase     = basicAmount.min(PF_BASIC_CAP);
        BigDecimal pfEmployee = pfBase.multiply(PF_RATE).min(PF_MAX_MONTHLY).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pfEmployer = pfEmployee;

        // ── ESI (only if monthly gross <= 21,000) ────────────────────────────
        boolean esiApplicable  = monthlyGross.compareTo(ESI_GROSS_LIMIT) <= 0;
        BigDecimal esiEmployee = esiApplicable
                ? monthlyGross.multiply(ESI_EMP_RATE).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal esiEmployer = esiApplicable
                ? monthlyGross.multiply(ESI_EMP_R_RATE).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        // ── Professional Tax (flat, reads a 'PT' catalog override if configured) ─
        BigDecimal pt = profTax(monthlyGross);

        BigDecimal netSalary = monthlyGross.subtract(pfEmployee).subtract(esiEmployee).subtract(pt);

        SalaryStructure structure = SalaryStructure.builder()
                .employee(employee)
                .annualCtc(annualCtc)
                .ctc(annualCtc)
                .effectiveFrom(effectiveFrom)
                .effectiveTo(null)
                .grossSalary(monthlyGross)
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
        structure = salaryStructureRepository.save(structure);

        List<SalaryStructureComponent> componentRows = new ArrayList<>();
        for (ResolvedComponent r : resolved) {
            componentRows.add(SalaryStructureComponent.builder()
                    .salaryStructure(structure)
                    .payrollComponent(r.component())
                    .calculationType(r.calculationType())
                    .value(r.value())
                    .computedAmount(amountByComponentId.get(r.component().getId()))
                    .build());
        }
        salaryStructureComponentRepository.saveAll(componentRows);

        return structure;
    }

    /** Rejects self-reference, references to non-PERCENTAGE-eligible components, and chaining beyond one level. */
    private void validateChainingAndResiduals(List<ResolvedComponent> resolved) {
        long formulaCount = resolved.stream().filter(r -> r.calculationType() == CalculationType.FORMULA).count();
        if (formulaCount > 1) {
            throw new ValidationException("At most one earning component may be marked FORMULA (residual) per salary structure");
        }
        for (ResolvedComponent r : resolved) {
            if (r.calculationType() != CalculationType.PERCENTAGE || r.component().getPercentageOfComponent() == null) {
                continue;
            }
            PayrollComponent ref = r.component().getPercentageOfComponent();
            if (ref.getId().equals(r.component().getId())) {
                throw new ValidationException("Component '" + r.component().getName() + "' cannot be a percentage of itself");
            }
            if (ref.getCalculationType() == CalculationType.FORMULA) {
                throw new ValidationException("Component '" + r.component().getName()
                        + "' cannot be a percentage of '" + ref.getName() + "', which is a FORMULA/residual component");
            }
            if (ref.getPercentageOfComponent() != null) {
                throw new ValidationException("Component '" + r.component().getName()
                        + "' references '" + ref.getName() + "', which itself references another component"
                        + " — chaining beyond one level isn't supported");
            }
        }
    }

    private static BigDecimal pctOf(BigDecimal base, BigDecimal percentValue, PayrollComponent component) {
        if (percentValue == null) {
            throw new ValidationException("Component '" + component.getName() + "' is PERCENTAGE type but has no value configured");
        }
        return base.multiply(percentValue.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Professional Tax: flat ₹200/month if gross > ₹10,000, or a configured 'PT' catalog override. */
    private BigDecimal profTax(BigDecimal monthlyGross) {
        if (monthlyGross.compareTo(PT_THRESHOLD) <= 0) return BigDecimal.ZERO;
        return payrollComponentRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .filter(c -> "PT".equals(c.getCode()) && c.getCalculationType() == CalculationType.FIXED && c.getValue() != null)
                .findFirst()
                .map(c -> c.getValue().setScale(2, RoundingMode.HALF_UP))
                .orElse(PT_AMOUNT);
    }

    private SalaryStructureResponse toResponseWithComponents(SalaryStructure structure) {
        SalaryStructureResponse response = salaryStructureMapper.toResponse(structure);
        response.setComponents(salaryStructureComponentMapper.toResponseList(
                salaryStructureComponentRepository.findBySalaryStructureId(structure.getId())));
        return response;
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

    private static BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
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
