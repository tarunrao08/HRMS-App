package com.hrms.employee.repository;

import com.hrms.employee.entity.Designation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DesignationRepository extends JpaRepository<Designation, UUID> {
    boolean existsByNameIgnoreCaseAndDepartmentId(String name, UUID departmentId);
    boolean existsByNameIgnoreCaseAndDepartmentIdAndIdNot(String name, UUID departmentId, UUID id);
    List<Designation> findByDepartmentId(UUID departmentId);
}
