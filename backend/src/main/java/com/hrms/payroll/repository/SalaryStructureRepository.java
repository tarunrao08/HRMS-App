package com.hrms.payroll.repository;

import com.hrms.employee.enums.EmploymentStatus;
import com.hrms.payroll.entity.SalaryStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SalaryStructureRepository extends JpaRepository<SalaryStructure, UUID> {

    List<SalaryStructure> findByEmployeeIdOrderByEffectiveFromDesc(UUID employeeId);

    @Query("SELECT s FROM SalaryStructure s WHERE s.employee.id = :empId " +
           "AND s.effectiveFrom <= :date " +
           "AND (s.effectiveTo IS NULL OR s.effectiveTo >= :date) " +
           "AND s.active = TRUE " +
           "ORDER BY s.effectiveFrom DESC")
    List<SalaryStructure> findActiveForEmployee(@Param("empId") UUID empId, @Param("date") LocalDate date);

    @Query("SELECT s FROM SalaryStructure s JOIN s.employee e " +
           "WHERE s.active = TRUE AND e.employmentStatus = :status")
    List<SalaryStructure> findActiveForEmployeesByStatus(@Param("status") EmploymentStatus status);
}
