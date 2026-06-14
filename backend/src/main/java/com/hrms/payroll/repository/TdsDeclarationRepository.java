package com.hrms.payroll.repository;

import com.hrms.payroll.entity.TdsDeclaration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TdsDeclarationRepository extends JpaRepository<TdsDeclaration, UUID> {

    Optional<TdsDeclaration> findByEmployeeIdAndFinancialYear(UUID employeeId, String financialYear);

    List<TdsDeclaration> findByEmployeeIdOrderByFinancialYearDesc(UUID employeeId);
}
