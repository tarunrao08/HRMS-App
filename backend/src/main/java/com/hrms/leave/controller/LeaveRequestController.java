package com.hrms.leave.controller;

import com.hrms.auth.entity.User;
import com.hrms.auth.repository.UserRepository;
import com.hrms.common.dto.ApiResponse;
import com.hrms.common.dto.PageableResponse;
import com.hrms.common.exception.AppException;
import com.hrms.leave.dto.*;
import com.hrms.leave.enums.LeaveRequestStatus;
import com.hrms.leave.service.LeaveRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/leave/requests")
@RequiredArgsConstructor
@Tag(name = "Leave Requests", description = "Apply for leave, approval workflow, and admin view")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;
    private final UserRepository      userRepository;

    // ── Item 6: Apply for leave ───────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit a leave request (current user)")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> submit(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ApplyLeaveRequest request) {
        UUID employeeId = resolveEmployeeId(userDetails);
        LeaveRequestResponse response = leaveRequestService.submit(employeeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Leave request submitted successfully", response));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get the current user's leave requests (paginated)")
    public ResponseEntity<ApiResponse<PageableResponse<LeaveRequestResponse>>> getMyRequests(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) LeaveRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID employeeId = resolveEmployeeId(userDetails);
        var pageable = PageRequest.of(page, size, Sort.by("appliedAt").descending());
        return ResponseEntity.ok(ApiResponse.paginated(
                leaveRequestService.getMyRequests(employeeId, status, pageable)));
    }

    // ── Item 7: Approval workflow ─────────────────────────────────────────────

    @GetMapping("/pending-approval")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'MANAGER')")
    @Operation(summary = "Get leave requests pending the current user's approval")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> getPendingApprovals(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID approverId = resolveEmployeeId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(leaveRequestService.getPendingApprovals(approverId)));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'MANAGER')")
    @Operation(summary = "Approve a leave request at the current level")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> approve(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false) ApproveRejectRequest request) {
        UUID approverId = resolveEmployeeId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Leave request approved",
                leaveRequestService.approve(id, approverId, request)));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'MANAGER')")
    @Operation(summary = "Reject a leave request at the current level")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> reject(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false) ApproveRejectRequest request) {
        UUID approverId = resolveEmployeeId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Leave request rejected",
                leaveRequestService.reject(id, approverId, request)));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel a leave request (PENDING or APPROVED with future start date)")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = resolveUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Leave request cancelled",
                leaveRequestService.cancel(id, userId)));
    }

    // ── Item 8: Admin view ────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Get all leave requests with optional filters (HR Admin only)")
    public ResponseEntity<ApiResponse<PageableResponse<LeaveRequestResponse>>> getAllRequests(
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) LeaveRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("appliedAt").descending());
        return ResponseEntity.ok(ApiResponse.paginated(
                leaveRequestService.getAllRequests(employeeId, status, pageable)));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private UUID resolveEmployeeId(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .map(User::getEmployeeId)
                .filter(id -> id != null)
                .orElseThrow(() -> new AppException(
                        "No employee record linked to this user account",
                        HttpStatus.UNPROCESSABLE_ENTITY, "NO_EMPLOYEE_LINKED"));
    }

    private UUID resolveUserId(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .map(User::getId)
                .orElseThrow(() -> new AppException(
                        "User not found", HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND"));
    }
}
