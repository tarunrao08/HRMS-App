package com.hrms.payroll.service;

import com.hrms.payroll.dto.TdsDeclarationRequest;
import com.hrms.payroll.dto.TdsDeclarationResponse;

import java.util.List;
import java.util.UUID;

public interface TdsDeclarationService {

    TdsDeclarationResponse create(TdsDeclarationRequest request);

    List<TdsDeclarationResponse> getByEmployee(UUID employeeId);

    TdsDeclarationResponse getByEmployeeAndYear(UUID employeeId, String financialYear);

    TdsDeclarationResponse update(UUID id, TdsDeclarationRequest request);

    TdsDeclarationResponse verify(UUID id);

    java.util.Optional<TdsDeclarationResponse> findMyDeclaration(UUID employeeId, String financialYear);
}
