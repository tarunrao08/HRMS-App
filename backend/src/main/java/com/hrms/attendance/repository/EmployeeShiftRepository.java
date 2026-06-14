package com.hrms.attendance.repository;

import com.hrms.attendance.entity.EmployeeShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmployeeShiftRepository extends JpaRepository<EmployeeShift, UUID> {

    List<EmployeeShift> findByEmployeeId(UUID employeeId);

    @Query("SELECT es FROM EmployeeShift es " +
           "WHERE es.employee.id = :employeeId " +
           "AND es.effectiveFrom <= :date " +
           "AND (es.effectiveTo IS NULL OR es.effectiveTo >= :date) " +
           "ORDER BY es.effectiveFrom DESC")
    List<EmployeeShift> findActiveShiftForEmployee(@Param("employeeId") UUID employeeId,
                                                   @Param("date") LocalDate date);
}
