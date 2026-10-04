package com.hrms.attendance.repository;

import com.hrms.attendance.entity.AttendanceDayLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceDayLedgerRepository extends JpaRepository<AttendanceDayLedger, UUID> {

    Optional<AttendanceDayLedger> findByEmployeeIdAndLedgerDate(UUID employeeId, LocalDate ledgerDate);

    List<AttendanceDayLedger> findByEmployeeIdAndLedgerDateBetween(UUID employeeId, LocalDate from, LocalDate to);
}
