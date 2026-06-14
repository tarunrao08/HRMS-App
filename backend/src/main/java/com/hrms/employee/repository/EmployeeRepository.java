package com.hrms.employee.repository;

import com.hrms.employee.entity.Employee;
import com.hrms.employee.enums.EmploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID>, JpaSpecificationExecutor<Employee> {
    Optional<Employee> findByEmployeeCode(String code);
    Optional<Employee> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, UUID id);
    List<Employee> findByManagerId(UUID managerId);

    List<Employee> findByEmploymentStatusOrderByFirstNameAscLastNameAsc(EmploymentStatus status);

    @Query("SELECT MAX(e.employeeCode) FROM Employee e WHERE e.employeeCode LIKE 'EMP%'")
    Optional<String> findMaxEmployeeCode();
}
