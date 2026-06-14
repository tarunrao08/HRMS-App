package com.hrms.payroll.service;

import com.hrms.payroll.dto.PayableSummaryResponse;
import com.hrms.payroll.dto.SalaryStructureRequest;
import com.hrms.payroll.dto.SalaryStructureResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SalaryStructureService {

    SalaryStructureResponse create(SalaryStructureRequest request);

    SalaryStructureResponse getById(UUID id);

    List<SalaryStructureResponse> getByEmployee(UUID employeeId);

    SalaryStructureResponse getActiveForEmployee(UUID employeeId);

    Optional<SalaryStructureResponse> getMyActiveSalaryStructure(UUID employeeId);

    PayableSummaryResponse getTotalPayableSummary();

    SalaryStructureResponse update(UUID id, SalaryStructureRequest request);

    void delete(UUID id);
}
