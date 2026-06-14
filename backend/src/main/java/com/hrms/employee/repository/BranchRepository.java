package com.hrms.employee.repository;

import com.hrms.employee.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BranchRepository extends JpaRepository<Branch, UUID> {
    boolean existsByNameIgnoreCase(String name);
}
