package com.hrms.employee.service.impl;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.exception.ValidationException;
import com.hrms.employee.dto.DepartmentRequest;
import com.hrms.employee.dto.DepartmentResponse;
import com.hrms.employee.entity.Department;
import com.hrms.employee.mapper.DepartmentMapper;
import com.hrms.employee.repository.DepartmentRepository;
import com.hrms.employee.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper     departmentMapper;

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
        departmentRepository.delete(department);
        log.info("Deleted department: {}", department.getName());
    }

    private Department findOrThrow(UUID id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id.toString()));
    }
}
