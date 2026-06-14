package com.hrms.employee.repository;

import com.hrms.employee.entity.Employee;
import com.hrms.employee.enums.EmploymentStatus;
import com.hrms.employee.enums.EmploymentType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EmployeeSpecification {

    private EmployeeSpecification() {}

    public static Specification<Employee> filter(String search, UUID departmentId, UUID designationId,
                                                  UUID branchId, UUID managerId,
                                                  EmploymentStatus status, EmploymentType type) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("firstName")), pattern),
                    cb.like(cb.lower(root.get("lastName")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("employeeCode")), pattern)
                ));
            }
            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("id"), departmentId));
            }
            if (designationId != null) {
                predicates.add(cb.equal(root.get("designation").get("id"), designationId));
            }
            if (branchId != null) {
                predicates.add(cb.equal(root.get("branch").get("id"), branchId));
            }
            if (managerId != null) {
                predicates.add(cb.equal(root.get("manager").get("id"), managerId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("employmentStatus"), status));
            } else {
                // Default: hide soft-deleted (INACTIVE) employees unless explicitly requested
                predicates.add(cb.notEqual(root.get("employmentStatus"), EmploymentStatus.INACTIVE));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("employmentType"), type));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
