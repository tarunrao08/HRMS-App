package com.hrms.employee.service.impl;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.exception.ValidationException;
import com.hrms.employee.dto.DesignationRequest;
import com.hrms.employee.dto.DesignationResponse;
import com.hrms.employee.entity.Department;
import com.hrms.employee.entity.Designation;
import com.hrms.employee.mapper.DesignationMapper;
import com.hrms.employee.repository.DepartmentRepository;
import com.hrms.employee.repository.DesignationRepository;
import com.hrms.employee.service.DesignationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DesignationServiceImpl implements DesignationService {

    private final DesignationRepository designationRepository;
    private final DepartmentRepository  departmentRepository;
    private final DesignationMapper     designationMapper;

    @Override
    @Transactional
    public DesignationResponse create(DesignationRequest request) {
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId().toString()));

        if (designationRepository.existsByNameIgnoreCaseAndDepartmentId(request.getName(), department.getId())) {
            throw new ValidationException("Designation '" + request.getName() + "' already exists in this department");
        }

        Designation designation = designationMapper.toEntity(request);
        designation.setDepartment(department);
        designation = designationRepository.save(designation);
        log.info("Created designation: {}", designation.getName());
        return designationMapper.toResponse(designation);
    }

    @Override
    @Transactional(readOnly = true)
    public DesignationResponse getById(UUID id) {
        return designationMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DesignationResponse> getAll(UUID departmentId) {
        List<Designation> list = (departmentId != null)
                ? designationRepository.findByDepartmentId(departmentId)
                : designationRepository.findAll();
        return list.stream().map(designationMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public DesignationResponse update(UUID id, DesignationRequest request) {
        Designation designation = findOrThrow(id);

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId().toString()));

        boolean nameOrDeptChanged = !designation.getName().equalsIgnoreCase(request.getName())
                || !department.getId().equals(designation.getDepartment() == null ? null : designation.getDepartment().getId());
        if (nameOrDeptChanged && designationRepository.existsByNameIgnoreCaseAndDepartmentIdAndIdNot(
                request.getName(), department.getId(), id)) {
            throw new ValidationException("Designation '" + request.getName() + "' already exists in this department");
        }

        designationMapper.updateEntity(designation, request);
        designation.setDepartment(department);
        designation = designationRepository.save(designation);
        log.info("Updated designation: {}", designation.getName());
        return designationMapper.toResponse(designation);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Designation designation = findOrThrow(id);
        designationRepository.delete(designation);
        log.info("Deleted designation: {}", designation.getName());
    }

    private Designation findOrThrow(UUID id) {
        return designationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Designation", "id", id.toString()));
    }
}
