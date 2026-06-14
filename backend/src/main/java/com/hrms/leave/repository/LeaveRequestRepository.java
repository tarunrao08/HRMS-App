package com.hrms.leave.repository;

import com.hrms.leave.entity.LeaveRequest;
import com.hrms.leave.enums.LeaveRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID>, JpaSpecificationExecutor<LeaveRequest> {

    Page<LeaveRequest> findByEmployeeId(UUID employeeId, Pageable pageable);

    Page<LeaveRequest> findByEmployeeIdAndStatus(UUID employeeId, LeaveRequestStatus status, Pageable pageable);

    List<LeaveRequest> findByCurrentApproverIdAndStatus(UUID currentApproverId, LeaveRequestStatus status);

    Page<LeaveRequest> findByStatus(LeaveRequestStatus status, Pageable pageable);

    @Query("""
            SELECT lr FROM LeaveRequest lr
            WHERE lr.status = com.hrms.leave.enums.LeaveRequestStatus.PENDING
              AND lr.employee.manager.id = :managerId
              AND (
                  EXISTS (
                      SELECT 1 FROM LeaveApproval la
                      WHERE la.leaveRequest.id = lr.id
                        AND la.approver.id = :managerId
                        AND la.approverLevel = lr.currentApprovalLevel
                        AND la.status = com.hrms.leave.enums.LeaveApprovalStatus.PENDING
                  )
                  OR NOT EXISTS (
                      SELECT 1 FROM LeaveApproval la
                      WHERE la.leaveRequest.id = lr.id
                  )
              )
            ORDER BY lr.appliedAt ASC
            """)
    List<LeaveRequest> findPendingForManager(@Param("managerId") UUID managerId);

    // Used to detect overlapping approved/pending leaves before applying a new request
    @Query("""
            SELECT lr FROM LeaveRequest lr
            WHERE lr.employee.id = :employeeId
              AND lr.status IN ('PENDING', 'APPROVED')
              AND lr.startDate <= :endDate
              AND lr.endDate   >= :startDate
            """)
    List<LeaveRequest> findOverlapping(
            @Param("employeeId") UUID employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
