package com.hrms.leave.repository;

import com.hrms.leave.entity.LeaveApproval;
import com.hrms.leave.entity.LeaveRequest;
import com.hrms.leave.enums.LeaveRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveApprovalRepository extends JpaRepository<LeaveApproval, UUID> {

    List<LeaveApproval> findByLeaveRequestIdOrderByApproverLevelAsc(UUID leaveRequestId);

    Optional<LeaveApproval> findByLeaveRequestIdAndApproverLevel(UUID leaveRequestId, int approverLevel);

    @Query("""
            SELECT la.leaveRequest FROM LeaveApproval la
            WHERE la.approver.id = :approverId
              AND la.status = com.hrms.leave.enums.LeaveApprovalStatus.PENDING
              AND la.approverLevel = la.leaveRequest.currentApprovalLevel
              AND la.leaveRequest.status = com.hrms.leave.enums.LeaveRequestStatus.PENDING
            ORDER BY la.leaveRequest.appliedAt ASC
            """)
    List<LeaveRequest> findPendingLeaveRequestsForApprover(@Param("approverId") UUID approverId);
}
