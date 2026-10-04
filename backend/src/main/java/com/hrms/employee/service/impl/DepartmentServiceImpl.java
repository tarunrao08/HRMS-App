package com.hrms.employee.service.impl;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.exception.ValidationException;
import com.hrms.employee.dto.DepartmentRequest;
import com.hrms.employee.dto.DepartmentResponse;
import com.hrms.employee.entity.Branch;
import com.hrms.employee.entity.Department;
import com.hrms.employee.mapper.DepartmentMapper;
import com.hrms.employee.repository.BranchRepository;
import com.hrms.employee.repository.DepartmentRepository;
import com.hrms.employee.repository.DesignationRepository;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.employee.service.DepartmentService;
import com.hrms.onboarding.repository.OnboardingTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final BranchRepository     branchRepository;
    private final DepartmentMapper     departmentMapper;
    private final EmployeeRepository   employeeRepository;
    private final DesignationRepository designationRepository;
    private final OnboardingTemplateRepository onboardingTemplateRepository;

    @Override
    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        if (departmentRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ValidationException("Department with name '" + request.getName() + "' already exists");
        }
        Department department = departmentMapper.toEntity(request);
        department = departmentRepository.save(department);
        log.info("Created department: {}", department.getName());
        return departmentMapper.toResponse(department);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getById(UUID id) {
        return departmentMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAll() {
        return departmentRepository.findAll().stream()
                .map(departmentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAll(UUID branchId) {
        if (branchId == null) {
            return getAll();
        }
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", "id", branchId.toString()));
        return branch.getDepartments().stream()
                .sorted(Comparator.comparing(Department::getName, String.CASE_INSENSITIVE_ORDER))
                .map(departmentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public DepartmentResponse update(UUID id, DepartmentRequest request) {
        Department department = findOrThrow(id);

        boolean nameChanged = !department.getName().equalsIgnoreCase(request.getName());
        if (nameChanged && departmentRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ValidationException("Department with name '" + request.getName() + "' already exists");
        }

        departmentMapper.updateEntity(department, request);
        department = departmentRepository.save(department);
        log.info("Updated department: {}", department.getName());
        return departmentMapper.toResponse(department);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Department department = findOrThrow(id);

        long employeeCount = employeeRepository.countByDepartmentId(id);
        if (employeeCount > 0) {
            throw new ValidationException("Cannot delete department '" + department.getName() + "': "
                    + employeeCount + " employee(s) are assigned to it");
        }
        long designationCount = designationRepository.countByDepartmentId(id);
        if (designationCount > 0) {
            throw new ValidationException("Cannot delete department '" + department.getName() + "': "
                    + designationCount + " designation(s) belong to it");
        }
        long templateCount = onboardingTemplateRepository.countByDepartmentId(id);
        if (templateCount > 0) {
            throw new ValidationException("Cannot delete department '" + department.getName() + "': "
                    + templateCount + " onboarding template(s) reference it");
        }

        departmentRepository.delete(department);
        log.info("Deleted department: {}", department.getName());
    }

    private Department findOrThrow(UUID id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id.toString()));
    }
}
