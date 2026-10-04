package com.hrms.employee.repository;

import com.hrms.employee.entity.EmployeeFamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeFamilyMemberRepository extends JpaRepository<EmployeeFamilyMember, UUID> {
    List<EmployeeFamilyMember> findByEmployeeId(UUID employeeId);
    Optional<EmployeeFamilyMember> findByIdAndEmployeeId(UUID id, UUID employeeId);
    void deleteByEmployeeId(UUID employeeId);
}
