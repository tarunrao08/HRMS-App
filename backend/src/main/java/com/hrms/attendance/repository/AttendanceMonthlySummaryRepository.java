package com.hrms.attendance.repository;

import com.hrms.attendance.entity.AttendanceMonthlySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceMonthlySummaryRepository extends JpaRepository<AttendanceMonthlySummary, UUID> {

    Optional<AttendanceMonthlySummary> findByEmployeeIdAndYearAndMonth(UUID employeeId, int year, int month);

    List<AttendanceMonthlySummary> findByEmployeeIdOrderByYearDescMonthDesc(UUID employeeId);

    List<AttendanceMonthlySummary> findByYearAndMonth(int year, int month);
}
