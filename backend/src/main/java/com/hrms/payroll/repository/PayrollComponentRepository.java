package com.hrms.payroll.repository;

import com.hrms.payroll.entity.PayrollComponent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PayrollComponentRepository extends JpaRepository<PayrollComponent, UUID> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByCode(String code);

    List<PayrollComponent> findByActiveTrueOrderByDisplayOrderAsc();
}
