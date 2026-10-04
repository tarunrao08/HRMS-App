package com.hrms.leave.repository;

import com.hrms.leave.entity.LeaveBalanceAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LeaveBalanceAdjustmentRepository extends JpaRepository<LeaveBalanceAdjustment, UUID> {
    List<LeaveBalanceAdjustment> findByLeaveBalanceIdOrderByCreatedAtDesc(UUID leaveBalanceId);
}
