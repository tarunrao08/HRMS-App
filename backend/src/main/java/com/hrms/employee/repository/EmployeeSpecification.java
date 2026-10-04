package com.hrms.employee.repository;

import com.hrms.employee.entity.Employee;
import com.hrms.employee.enums.EmploymentStatus;
import com.hrms.employee.enums.EmploymentType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EmployeeSpecification {

    private EmployeeSpecification() {}

    public static Specification<Employee> filter(String search, UUID departmentId, UUID designationId,
                                                  UUID branchId, UUID managerId,
                                                  EmploymentStatus status, EmploymentType type) {
        return filter(search, departmentId, designationId, branchId, managerId, status, type, Sort.Direction.ASC);
    }

    /**
     * Same filtering as above, plus a case-insensitive name ordering (LOWER(firstName), LOWER(lastName)).
     * Ordering is applied here - rather than via Pageable's Sort - because Sort only supports plain
     * property paths and can't express LOWER(...). The caller must pass an unsorted Pageable, otherwise
     * Spring Data would overwrite this ordering with a plain (case-sensitive) one.
     */
    public static Specification<Employee> filter(String search, UUID departmentId, UUID designationId,
                                                  UUID branchId, UUID managerId,
                                                  EmploymentStatus status, EmploymentType type,
                                                  Sort.Direction direction) {
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

            // Skip ordering on the count(*) query Spring Data issues for pagination totals.
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                Order firstNameOrder = direction == Sort.Direction.DESC
                        ? cb.desc(cb.lower(root.get("firstName")))
                        : cb.asc(cb.lower(root.get("firstName")));
                Order lastNameOrder = direction == Sort.Direction.DESC
                        ? cb.desc(cb.lower(root.get("lastName")))
                        : cb.asc(cb.lower(root.get("lastName")));
                query.orderBy(List.of(firstNameOrder, lastNameOrder));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
