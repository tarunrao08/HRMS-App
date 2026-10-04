package com.hrms.payroll.service.impl;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.exception.ValidationException;
import com.hrms.payroll.dto.PayrollComponentRequest;
import com.hrms.payroll.dto.PayrollComponentResponse;
import com.hrms.payroll.entity.PayrollComponent;
import com.hrms.payroll.enums.CalculationType;
import com.hrms.payroll.mapper.PayrollComponentMapper;
import com.hrms.payroll.repository.PayrollComponentRepository;
import com.hrms.payroll.repository.PayslipComponentRepository;
import com.hrms.payroll.repository.SalaryStructureComponentRepository;
import com.hrms.payroll.service.PayrollComponentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PayrollComponentServiceImpl implements PayrollComponentService {

    private final PayrollComponentRepository payrollComponentRepository;
    private final SalaryStructureComponentRepository salaryStructureComponentRepository;
    private final PayslipComponentRepository payslipComponentRepository;
    private final PayrollComponentMapper payrollComponentMapper;

    @Override
    public PayrollComponentResponse create(PayrollComponentRequest request) {
        if (payrollComponentRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ValidationException("Payroll component with name '" + request.getName() + "' already exists");
        }
        if (payrollComponentRepository.existsByCode(request.getCode())) {
            throw new ValidationException("Payroll component with code '" + request.getCode() + "' already exists");
        }
        PayrollComponent component = payrollComponentMapper.toEntity(request);
        component.setPercentageOfComponent(resolvePercentageOf(request, null));
        component = payrollComponentRepository.save(component);
        log.info("Created payroll component: {} ({})", component.getName(), component.getCode());
        return payrollComponentMapper.toResponse(component);
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollComponentResponse getById(UUID id) {
        return payrollComponentMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollComponentResponse> getAll() {
        return payrollComponentRepository.findAll().stream()
                .map(payrollComponentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollComponentResponse> getActive() {
        return payrollComponentRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(payrollComponentMapper::toResponse)
                .toList();
    }

    @Override
    public PayrollComponentResponse update(UUID id, PayrollComponentRequest request) {
        PayrollComponent component = findOrThrow(id);
        if (!component.getName().equalsIgnoreCase(request.getName())
                && payrollComponentRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ValidationException("Payroll component with name '" + request.getName() + "' already exists");
        }
        if (!component.getCode().equals(request.getCode())
                && payrollComponentRepository.existsByCode(request.getCode())) {
            throw new ValidationException("Payroll component with code '" + request.getCode() + "' already exists");
        }
        payrollComponentMapper.updateEntity(component, request);
        component.setPercentageOfComponent(resolvePercentageOf(request, component));
        component = payrollComponentRepository.save(component);
        log.info("Updated payroll component: {}", component.getId());
        return payrollComponentMapper.toResponse(component);
    }

    @Override
    public void delete(UUID id) {
        PayrollComponent component = findOrThrow(id);
        if (salaryStructureComponentRepository.existsByPayrollComponentId(id)
                || payslipComponentRepository.existsByPayrollComponentId(id)) {
            throw new ValidationException(
                    "Cannot delete '" + component.getName() + "' — it is used by existing salary structures or payslips."
                    + " Deactivate it instead to keep it out of new structures.");
        }
        List<PayrollComponent> dependents = payrollComponentRepository.findByPercentageOfComponentId(id);
        if (!dependents.isEmpty()) {
            throw new ValidationException(
                    "Cannot delete '" + component.getName() + "' — " + dependents.get(0).getName()
                    + (dependents.size() > 1 ? " and " + (dependents.size() - 1) + " other component(s)" : "")
                    + " are configured as a percentage of it.");
        }
        payrollComponentRepository.delete(component);
        log.info("Deleted payroll component: {}", id);
    }

    private PayrollComponent resolvePercentageOf(PayrollComponentRequest request, PayrollComponent self) {
        UUID refId = request.getPercentageOfComponentId();
        if (refId == null) return null;
        if (request.getCalculationType() != CalculationType.PERCENTAGE) {
            throw new ValidationException("percentageOfComponentId can only be set when calculationType is PERCENTAGE");
        }
        if (self != null && refId.equals(self.getId())) {
            throw new ValidationException("A component cannot be a percentage of itself");
        }
        PayrollComponent ref = payrollComponentRepository.findById(refId)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollComponent", "id", refId.toString()));
        if (ref.getPercentageOfComponent() != null) {
            throw new ValidationException(
                    "'" + ref.getName() + "' is itself a percentage of another component — chaining beyond one level isn't supported");
        }
        if (self != null && !payrollComponentRepository.findByPercentageOfComponentId(self.getId()).isEmpty()) {
            throw new ValidationException(
                    "Other components are already configured as a percentage of '" + self.getName()
                    + "' — making it a percentage of another component too would chain beyond one level");
        }
        return ref;
    }

    private PayrollComponent findOrThrow(UUID id) {
        return payrollComponentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollComponent", "id", id.toString()));
    }
}
