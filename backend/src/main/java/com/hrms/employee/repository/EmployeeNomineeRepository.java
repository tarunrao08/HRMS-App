package com.hrms.employee.repository;

import com.hrms.employee.entity.EmployeeNominee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeNomineeRepository extends JpaRepository<EmployeeNominee, UUID> {
    List<EmployeeNominee> findByEmployeeId(UUID employeeId);
    Optional<EmployeeNominee> findByIdAndEmployeeId(UUID id, UUID employeeId);
    void deleteByEmployeeId(UUID employeeId);
}
