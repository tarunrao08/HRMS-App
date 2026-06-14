package com.hrms.employee.service.impl;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.exception.ValidationException;
import com.hrms.employee.dto.BranchRequest;
import com.hrms.employee.dto.BranchResponse;
import com.hrms.employee.entity.Branch;
import com.hrms.employee.mapper.BranchMapper;
import com.hrms.employee.repository.BranchRepository;
import com.hrms.employee.service.BranchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;
    private final BranchMapper     branchMapper;

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

        branchMapper.updateEntity(branch, request);
        branch = branchRepository.save(branch);
        log.info("Updated branch: {}", branch.getName());
        return branchMapper.toResponse(branch);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Branch branch = findOrThrow(id);
        branchRepository.delete(branch);
        log.info("Deleted branch: {}", branch.getName());
    }

    private Branch findOrThrow(UUID id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", "id", id.toString()));
    }
}
