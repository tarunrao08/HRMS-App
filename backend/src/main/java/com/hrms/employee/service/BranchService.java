package com.hrms.employee.service;

import com.hrms.employee.dto.BranchRequest;
import com.hrms.employee.dto.BranchResponse;

import java.util.List;
import java.util.UUID;

public interface BranchService {
    BranchResponse create(BranchRequest request);
    BranchResponse getById(UUID id);
    List<BranchResponse> getAll();
    BranchResponse update(UUID id, BranchRequest request);
    void delete(UUID id);
}
