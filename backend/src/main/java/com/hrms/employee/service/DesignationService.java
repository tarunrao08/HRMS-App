package com.hrms.employee.service;

import com.hrms.employee.dto.DesignationRequest;
import com.hrms.employee.dto.DesignationResponse;

import java.util.List;
import java.util.UUID;

public interface DesignationService {
    DesignationResponse create(DesignationRequest request);
    DesignationResponse getById(UUID id);
    List<DesignationResponse> getAll(UUID departmentId);
    DesignationResponse update(UUID id, DesignationRequest request);
    void delete(UUID id);
}
