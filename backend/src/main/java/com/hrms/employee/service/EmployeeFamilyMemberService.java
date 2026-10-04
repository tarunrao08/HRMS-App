package com.hrms.employee.service;

import com.hrms.employee.dto.FamilyMemberRequest;
import com.hrms.employee.dto.FamilyMemberResponse;

import java.util.List;
import java.util.UUID;

public interface EmployeeFamilyMemberService {
    List<FamilyMemberResponse> list(UUID employeeId);
    FamilyMemberResponse add(UUID employeeId, FamilyMemberRequest request);
    FamilyMemberResponse update(UUID employeeId, UUID memberId, FamilyMemberRequest request);
    void delete(UUID employeeId, UUID memberId);
    List<FamilyMemberResponse> replaceAll(UUID employeeId, List<FamilyMemberRequest> requests);
}
