package com.hrms.employee.repository;

import com.hrms.employee.entity.Designation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DesignationRepository extends JpaRepository<Designation, UUID> {
    Optional<Designation> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    List<Designation> findByDepartmentId(UUID departmentId);
}
