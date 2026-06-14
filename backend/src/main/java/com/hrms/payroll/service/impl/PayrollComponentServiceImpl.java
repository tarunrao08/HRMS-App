package com.hrms.payroll.service.impl;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.exception.ValidationException;
import com.hrms.payroll.dto.PayrollComponentRequest;
import com.hrms.payroll.dto.PayrollComponentResponse;
import com.hrms.payroll.entity.PayrollComponent;
import com.hrms.payroll.mapper.PayrollComponentMapper;
import com.hrms.payroll.repository.PayrollComponentRepository;
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
        component = payrollComponentRepository.save(component);
        log.info("Updated payroll component: {}", component.getId());
        return payrollComponentMapper.toResponse(component);
    }

    @Override
    public void delete(UUID id) {
        PayrollComponent component = findOrThrow(id);
        payrollComponentRepository.delete(component);
        log.info("Deleted payroll component: {}", id);
    }

    private PayrollComponent findOrThrow(UUID id) {
        return payrollComponentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollComponent", "id", id.toString()));
    }
}
