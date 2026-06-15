package com.hrms.payroll.repository;

import com.hrms.payroll.entity.Payslip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayslipRepository extends JpaRepository<Payslip, UUID> {

    Optional<Payslip> findByEmployeeIdAndYearAndMonth(UUID employeeId, int year, int month);

    Page<Payslip> findByEmployeeId(UUID employeeId, Pageable pageable);

    List<Payslip> findByPayrollRunId(UUID payrollRunId);

    @Query("SELECT p FROM Payslip p JOIN FETCH p.employee WHERE p.id = :id")
    Optional<Payslip> findByIdWithEmployee(@Param("id") UUID id);

    @Query(value = """
            SELECT p FROM Payslip p
            WHERE p.employee.id = :employeeId
              AND (p.published = true
                   OR p.payrollRun.status = com.hrms.payroll.enums.PayrollRunStatus.APPROVED)
            ORDER BY p.year DESC, p.month DESC
            """)
    Page<Payslip> findLatestVisibleForEmployee(@Param("employeeId") UUID employeeId, Pageable pageable);
}
