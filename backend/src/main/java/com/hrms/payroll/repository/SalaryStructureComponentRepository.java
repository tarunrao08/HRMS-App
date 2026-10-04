package com.hrms.payroll.repository;

import com.hrms.payroll.entity.SalaryStructureComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SalaryStructureComponentRepository extends JpaRepository<SalaryStructureComponent, UUID> {

    List<SalaryStructureComponent> findBySalaryStructureId(UUID salaryStructureId);

    void deleteBySalaryStructureId(UUID salaryStructureId);

    boolean existsByPayrollComponentId(UUID payrollComponentId);
}
