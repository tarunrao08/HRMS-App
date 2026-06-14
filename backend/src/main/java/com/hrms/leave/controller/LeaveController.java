package com.hrms.leave.controller;

import com.hrms.common.dto.ApiResponse;
import com.hrms.common.dto.PageableResponse;
import com.hrms.leave.dto.*;
import com.hrms.leave.enums.LeaveRequestStatus;
import com.hrms.leave.service.LeaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
@Tag(name = "Leaves", description = "Apply, approve, reject and manage leave requests")
public class LeaveController {

    private final LeaveService leaveService;

    @PostMapping("/apply")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Apply for leave")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> apply(@Valid @RequestBody LeaveRequestRequest request) {
        LeaveRequestResponse response = leaveService.apply(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Leave request submitted successfully", response));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'MANAGER')")
    @Operation(summary = "Approve a leave request")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> approve(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false) LeaveApprovalRequest request) {
        UUID approverId = resolveUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Leave request approved",
                leaveService.approve(id, approverId, request != null ? request : new LeaveApprovalRequest())));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'MANAGER')")
    @Operation(summary = "Reject a leave request")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> reject(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false) LeaveApprovalRequest request) {
        UUID approverId = resolveUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Leave request rejected",
                leaveService.reject(id, approverId, request)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel a leave request")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID cancelledByUserId = resolveUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Leave request cancelled",
                leaveService.cancel(id, cancelledByUserId)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'MANAGER')")
    @Operation(summary = "Get all leave requests (admin/manager view)")
    public ResponseEntity<ApiResponse<PageableResponse<LeaveRequestResponse>>> getAllRequests(
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) LeaveRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("appliedAt").descending());
        return ResponseEntity.ok(ApiResponse.paginated(leaveService.getAllRequests(employeeId, status, pageable)));
    }

    @GetMapping("/employee/{employeeId}/balances")
    @Operation(summary = "Get leave balances for an employee for a given year")
    public ResponseEntity<ApiResponse<List<LeaveBalanceResponse>>> getBalances(
            @PathVariable UUID employeeId,
            @RequestParam int year) {
        return ResponseEntity.ok(ApiResponse.success(leaveService.getBalances(employeeId, year)));
    }

    @PostMapping("/employee/{employeeId}/balances/allocate")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Allocate or update leave balance for an employee")
    public ResponseEntity<ApiResponse<LeaveBalanceResponse>> allocateBalance(
            @PathVariable UUID employeeId,
            @RequestParam UUID leaveTypeId,
            @RequestParam int year,
            @RequestParam BigDecimal days) {
        LeaveBalanceResponse response = leaveService.allocateBalance(employeeId, leaveTypeId, year, days);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Leave balance allocated successfully", response));
    }

    @GetMapping("/employee/{employeeId}/requests")
    @Operation(summary = "Get paginated leave requests for an employee")
    public ResponseEntity<ApiResponse<PageableResponse<LeaveRequestResponse>>> getRequests(
            @PathVariable UUID employeeId,
            @RequestParam(required = false) LeaveRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("appliedAt").descending());
        return ResponseEntity.ok(ApiResponse.paginated(leaveService.getRequests(employeeId, status, pageable)));
    }

    @GetMapping("/pending-approvals")
    @Operation(summary = "Get all pending leave requests assigned to an approver")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> getPendingForApprover(
            @RequestParam UUID approverId) {
        return ResponseEntity.ok(ApiResponse.success(leaveService.getPendingForApprover(approverId)));
    }

    @GetMapping("/{id}/approvals")
    @Operation(summary = "Get approval history for a leave request")
    public ResponseEntity<ApiResponse<List<LeaveApprovalResponse>>> getApprovals(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(leaveService.getApprovals(id)));
    }

    private UUID resolveUserId(UserDetails userDetails) {
        if (userDetails == null) return null;
        try {
            return UUID.fromString(userDetails.getUsername());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
