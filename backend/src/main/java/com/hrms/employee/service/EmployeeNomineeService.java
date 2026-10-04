package com.hrms.employee.service;

import com.hrms.employee.dto.NomineeRequest;
import com.hrms.employee.dto.NomineeResponse;

import java.util.List;
import java.util.UUID;

public interface EmployeeNomineeService {
    List<NomineeResponse> list(UUID employeeId);
    NomineeResponse add(UUID employeeId, NomineeRequest request);
    NomineeResponse update(UUID employeeId, UUID nomineeId, NomineeRequest request);
    void delete(UUID employeeId, UUID nomineeId);
    List<NomineeResponse> replaceAll(UUID employeeId, List<NomineeRequest> requests);
}
