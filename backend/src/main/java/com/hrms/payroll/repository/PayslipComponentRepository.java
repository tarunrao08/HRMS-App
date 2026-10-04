package com.hrms.payroll.repository;

import com.hrms.payroll.entity.PayslipComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PayslipComponentRepository extends JpaRepository<PayslipComponent, UUID> {

    List<PayslipComponent> findByPayslipId(UUID payslipId);

    void deleteByPayslipId(UUID payslipId);

    boolean existsByPayrollComponentId(UUID payrollComponentId);
}
