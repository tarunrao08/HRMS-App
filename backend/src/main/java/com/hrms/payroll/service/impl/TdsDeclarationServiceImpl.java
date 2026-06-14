package com.hrms.payroll.service.impl;

import com.hrms.common.exception.BusinessException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.exception.ValidationException;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.payroll.dto.TdsDeclarationRequest;
import com.hrms.payroll.dto.TdsDeclarationResponse;
import com.hrms.payroll.entity.TdsDeclaration;
import com.hrms.payroll.mapper.TdsDeclarationMapper;
import com.hrms.payroll.repository.TdsDeclarationRepository;
import com.hrms.payroll.service.TdsDeclarationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TdsDeclarationServiceImpl implements TdsDeclarationService {

    private final TdsDeclarationRepository tdsDeclarationRepository;
    private final EmployeeRepository employeeRepository;
    private final TdsDeclarationMapper tdsDeclarationMapper;

    @Override
    public TdsDeclarationResponse create(TdsDeclarationRequest request) {
        if (tdsDeclarationRepository.findByEmployeeIdAndFinancialYear(
                request.getEmployeeId(), request.getFinancialYear()).isPresent()) {
            throw new ValidationException("TDS declaration for employee "
                    + request.getEmployeeId() + " and financial year "
                    + request.getFinancialYear() + " already exists");
        }
        Employee employee = findEmployeeOrThrow(request.getEmployeeId());
        TdsDeclaration declaration = tdsDeclarationMapper.toEntity(request);
        declaration.setEmployee(employee);
        declaration.setDeclaredAt(Instant.now());
        declaration.setVerified(false);
        declaration = tdsDeclarationRepository.save(declaration);
        log.info("Created TDS declaration for employee: {} FY: {}",
                employee.getId(), request.getFinancialYear());
        return tdsDeclarationMapper.toResponse(declaration);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TdsDeclarationResponse> getByEmployee(UUID employeeId) {
        findEmployeeOrThrow(employeeId);
        return tdsDeclarationRepository.findByEmployeeIdOrderByFinancialYearDesc(employeeId).stream()
                .map(tdsDeclarationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TdsDeclarationResponse getByEmployeeAndYear(UUID employeeId, String financialYear) {
        TdsDeclaration declaration = tdsDeclarationRepository
                .findByEmployeeIdAndFinancialYear(employeeId, financialYear)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "TdsDeclaration", "employeeId/financialYear",
                        employeeId + "/" + financialYear));
        return tdsDeclarationMapper.toResponse(declaration);
    }

    @Override
    public TdsDeclarationResponse update(UUID id, TdsDeclarationRequest request) {
        TdsDeclaration declaration = findOrThrow(id);
        if (declaration.isVerified()) {
            throw new BusinessException("Cannot update a TDS declaration that has already been verified by HR");
        }
        Employee employee = findEmployeeOrThrow(request.getEmployeeId());
        tdsDeclarationMapper.updateEntity(declaration, request);
        declaration.setEmployee(employee);
        declaration = tdsDeclarationRepository.save(declaration);
        log.info("Updated TDS declaration: {}", id);
        return tdsDeclarationMapper.toResponse(declaration);
    }

    @Override
    public TdsDeclarationResponse verify(UUID id) {
        TdsDeclaration declaration = findOrThrow(id);
        declaration.setVerified(true);
        declaration = tdsDeclarationRepository.save(declaration);
        log.info("Verified TDS declaration: {}", id);
        return tdsDeclarationMapper.toResponse(declaration);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TdsDeclarationResponse> findMyDeclaration(UUID employeeId, String financialYear) {
        return tdsDeclarationRepository
                .findByEmployeeIdAndFinancialYear(employeeId, financialYear)
                .map(tdsDeclarationMapper::toResponse);
    }

    private TdsDeclaration findOrThrow(UUID id) {
        return tdsDeclarationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TdsDeclaration", "id", id.toString()));
    }

    private Employee findEmployeeOrThrow(UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId.toString()));
    }
}
