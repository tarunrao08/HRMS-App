package com.hrms.leave.repository;

import com.hrms.leave.entity.LeaveTypeTenureTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaveTypeTenureTierRepository extends JpaRepository<LeaveTypeTenureTier, UUID> {

    List<LeaveTypeTenureTier> findByLeaveTypeIdOrderByMinYearsAsc(UUID leaveTypeId);

    @Query("""
            SELECT t FROM LeaveTypeTenureTier t
            WHERE t.leaveType.id = :leaveTypeId
              AND t.minYears <= :years
              AND (t.maxYears IS NULL OR t.maxYears > :years)
            """)
    Optional<LeaveTypeTenureTier> findMatchingTier(@Param("leaveTypeId") UUID leaveTypeId, @Param("years") int years);
}
