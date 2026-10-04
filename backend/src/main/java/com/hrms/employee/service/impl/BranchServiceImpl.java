package com.hrms.employee.service.impl;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.exception.ValidationException;
import com.hrms.employee.dto.BranchRequest;
import com.hrms.employee.dto.BranchResponse;
import com.hrms.employee.entity.Branch;
import com.hrms.employee.entity.Department;
import com.hrms.employee.mapper.BranchMapper;
import com.hrms.employee.repository.BranchRepository;
import com.hrms.employee.repository.DepartmentRepository;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.employee.service.BranchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {

    private final BranchRepository     branchRepository;
    private final DepartmentRepository departmentRepository;
    private final BranchMapper         branchMapper;
    private final EmployeeRepository   employeeRepository;

    @Override
    @Transactional
    public BranchResponse create(BranchRequest request) {
        if (branchRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ValidationException("Branch with name '" + request.getName() + "' already exists");
        }
        Branch branch = branchMapper.toEntity(request);
        if (branch.getCountry() == null || branch.getCountry().isBlank()) {
            branch.setCountry("India");
        }
        branch.setDepartments(resolveDepartments(request.getDepartmentIds()));
        branch = branchRepository.save(branch);
        log.info("Created branch: {}", branch.getName());
        return branchMapper.toResponse(branch);
    }

    @Override
    @Transactional(readOnly = true)
    public BranchResponse getById(UUID id) {
        return branchMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchResponse> getAll() {
        return branchRepository.findAll().stream()
                .map(branchMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public BranchResponse update(UUID id, BranchRequest request) {
        Branch branch = findOrThrow(id);

        boolean nameChanged = !branch.getName().equalsIgnoreCase(request.getName());
        if (nameChanged && branchRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ValidationException("Branch with name '" + request.getName() + "' already exists");
        }

        Set<Department> nextDepartments = resolveDepartments(request.getDepartmentIds());
        guardDepartmentsBeingRemoved(branch, nextDepartments);

        branchMapper.updateEntity(branch, request);
        branch.setDepartments(nextDepartments);
        branch = branchRepository.save(branch);
        log.info("Updated branch: {}", branch.getName());
        return branchMapper.toResponse(branch);
    }

    private Set<Department> resolveDepartments(List<UUID> departmentIds) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<UUID> requestedIds = new HashSet<>(departmentIds);
        List<Department> found = departmentRepository.findAllById(requestedIds);
        if (found.size() != requestedIds.size()) {
            Set<UUID> foundIds = new HashSet<>();
            found.forEach(d -> foundIds.add(d.getId()));
            UUID missing = requestedIds.stream().filter(reqId -> !foundIds.contains(reqId)).findFirst().orElseThrow();
            throw new ResourceNotFoundException("Department", "id", missing.toString());
        }
        return new HashSet<>(found);
    }

    /**
     * A department can't be dropped from a branch while employees are still assigned to that
     * exact branch + department combination — doing so would leave their record pointing at a
     * department/branch pairing that's no longer valid, silently breaking the flow that reads it.
     */
    private void guardDepartmentsBeingRemoved(Branch branch, Set<Department> nextDepartments) {
        for (Department current : branch.getDepartments()) {
            boolean stillPresent = nextDepartments.stream().anyMatch(d -> d.getId().equals(current.getId()));
            if (stillPresent) continue;
            long employeeCount = employeeRepository.countByBranchIdAndDepartmentId(branch.getId(), current.getId());
            if (employeeCount > 0) {
                throw new ValidationException("Cannot remove department '" + current.getName() + "' from branch '"
                        + branch.getName() + "': " + employeeCount + " employee(s) in that branch are assigned to it");
            }
        }
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Branch branch = findOrThrow(id);

        long employeeCount = employeeRepository.countByBranchId(id);
        if (employeeCount > 0) {
            throw new ValidationException("Cannot delete branch '" + branch.getName() + "': "
                    + employeeCount + " employee(s) are assigned to it");
        }

        branchRepository.delete(branch);
        log.info("Deleted branch: {}", branch.getName());
    }

    private Branch findOrThrow(UUID id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", "id", id.toString()));
    }
}
