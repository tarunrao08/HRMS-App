package com.hrms.employee.service.impl;

import com.hrms.common.dto.PageableResponse;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.exception.ValidationException;
import com.hrms.employee.dto.EmployeeNameResponse;
import com.hrms.employee.dto.EmployeeRequest;
import com.hrms.employee.dto.EmployeeResponse;
import com.hrms.employee.dto.EmployeeSummaryResponse;
import com.hrms.employee.entity.Branch;
import com.hrms.employee.entity.Department;
import com.hrms.employee.entity.Designation;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.enums.EmploymentStatus;
import com.hrms.employee.enums.EmploymentType;
import com.hrms.employee.mapper.EmployeeMapper;
import com.hrms.employee.repository.BranchRepository;
import com.hrms.employee.repository.DepartmentRepository;
import com.hrms.employee.repository.DesignationRepository;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.employee.repository.EmployeeSpecification;
import com.hrms.employee.event.EmployeeCreatedEvent;
import com.hrms.employee.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository       employeeRepository;
    private final DepartmentRepository     departmentRepository;
    private final DesignationRepository    designationRepository;
    private final BranchRepository         branchRepository;
    private final EmployeeMapper           employeeMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("An employee with email '" + request.getEmail() + "' already exists");
        }

        Employee employee = employeeMapper.toEntity(request);
        employee.setEmployeeCode(generateEmployeeCode());

        resolveRelations(employee, request);

        if (employee.getEmploymentStatus() == null) {
            employee.setEmploymentStatus(EmploymentStatus.ACTIVE);
        }
        if (employee.getEmploymentType() == null) {
            employee.setEmploymentType(EmploymentType.FULL_TIME);
        }

        employee = employeeRepository.save(employee);
        log.info("Created employee: {} ({})", employee.getEmployeeCode(), employee.getEmail());
        eventPublisher.publishEvent(new EmployeeCreatedEvent(this, employee.getId()));
        return employeeMapper.toResponse(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getById(UUID id) {
        return employeeMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getByCode(String code) {
        Employee employee = employeeRepository.findByEmployeeCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "code", code));
        return employeeMapper.toResponse(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public PageableResponse<EmployeeSummaryResponse> search(String search, UUID departmentId, UUID designationId,
                                                             UUID branchId, UUID managerId,
                                                             EmploymentStatus status, EmploymentType type,
                                                             Pageable pageable) {
        Specification<Employee> spec = EmployeeSpecification.filter(
                search, departmentId, designationId, branchId, managerId, status, type);
        Page<EmployeeSummaryResponse> page = employeeRepository.findAll(spec, pageable)
                .map(employeeMapper::toSummary);
        return PageableResponse.of(page);
    }

    @Override
    @Transactional
    public EmployeeResponse update(UUID id, EmployeeRequest request) {
        Employee employee = findOrThrow(id);

        if (employeeRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new ValidationException("An employee with email '" + request.getEmail() + "' already exists");
        }

        employeeMapper.updateEntity(employee, request);
        resolveRelations(employee, request);

        employee = employeeRepository.save(employee);
        log.info("Updated employee: {}", employee.getEmployeeCode());
        return employeeMapper.toResponse(employee);
    }

    @Override
    @Transactional
    public void deactivate(UUID id) {
        Employee employee = findOrThrow(id);
        employee.setEmploymentStatus(EmploymentStatus.INACTIVE);
        employeeRepository.save(employee);
        log.info("Deactivated employee: {}", employee.getEmployeeCode());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeSummaryResponse> getDirectReports(UUID managerId) {
        findOrThrow(managerId);
        return employeeRepository.findByManagerId(managerId).stream()
                .map(employeeMapper::toSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeNameResponse> getAllSummaries() {
        return employeeRepository.findAll().stream()
                .filter(e -> e.getEmploymentStatus() != EmploymentStatus.INACTIVE)
                .map(e -> new EmployeeNameResponse(e.getId(), e.getEmployeeCode(),
                        e.getFirstName() + " " + e.getLastName()))
                .toList();
    }

    private Employee findOrThrow(UUID id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id.toString()));
    }

    private void resolveRelations(Employee employee, EmployeeRequest request) {
        if (request.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId().toString()));
            employee.setDepartment(dept);
        } else {
            employee.setDepartment(null);
        }

        if (request.getDesignationId() != null) {
            Designation desig = designationRepository.findById(request.getDesignationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Designation", "id", request.getDesignationId().toString()));
            employee.setDesignation(desig);
        } else {
            employee.setDesignation(null);
        }

        if (request.getBranchId() != null) {
            Branch branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", "id", request.getBranchId().toString()));
            employee.setBranch(branch);
        } else {
            employee.setBranch(null);
        }

        if (request.getManagerId() != null) {
            Employee manager = employeeRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee (manager)", "id", request.getManagerId().toString()));
            employee.setManager(manager);
        } else {
            employee.setManager(null);
        }
    }

    private String generateEmployeeCode() {
        return employeeRepository.findMaxEmployeeCode()
                .map(last -> {
                    String numPart = last.substring(3);
                    int next = Integer.parseInt(numPart) + 1;
                    return String.format("EMP%05d", next);
                })
                .orElse("EMP00001");
    }
}
