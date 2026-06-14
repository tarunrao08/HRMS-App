package com.hrms.employee.service;

import com.hrms.common.dto.PageableResponse;
import com.hrms.employee.dto.EmployeeNameResponse;
import com.hrms.employee.dto.EmployeeRequest;
import com.hrms.employee.dto.EmployeeResponse;
import com.hrms.employee.dto.EmployeeSummaryResponse;
import com.hrms.employee.enums.EmploymentStatus;
import com.hrms.employee.enums.EmploymentType;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface EmployeeService {
    EmployeeResponse create(EmployeeRequest request);
    EmployeeResponse getById(UUID id);
    EmployeeResponse getByCode(String code);
    PageableResponse<EmployeeSummaryResponse> search(String search, UUID departmentId, UUID designationId,
                                                      UUID branchId, UUID managerId,
                                                      EmploymentStatus status, EmploymentType type,
                                                      Pageable pageable);
    EmployeeResponse update(UUID id, EmployeeRequest request);
    void deactivate(UUID id);
    List<EmployeeSummaryResponse> getDirectReports(UUID managerId);
    List<EmployeeNameResponse> getAllSummaries();
}
