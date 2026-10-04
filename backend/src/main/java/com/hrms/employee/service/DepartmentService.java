package com.hrms.employee.service;

import com.hrms.employee.dto.DepartmentRequest;
import com.hrms.employee.dto.DepartmentResponse;

import java.util.List;
import java.util.UUID;

public interface DepartmentService {
    DepartmentResponse create(DepartmentRequest request);
    DepartmentResponse getById(UUID id);
    List<DepartmentResponse> getAll();
    /** When branchId is given, returns only the departments offered by that branch. */
    List<DepartmentResponse> getAll(UUID branchId);
    DepartmentResponse update(UUID id, DepartmentRequest request);
    void delete(UUID id);
}
