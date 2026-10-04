package com.hrms.leave.controller;

import com.hrms.auth.entity.User;
import com.hrms.auth.repository.UserRepository;
import com.hrms.common.dto.ApiResponse;
import com.hrms.common.exception.AppException;
import com.hrms.leave.dto.AdjustLeaveBalanceRequest;
import com.hrms.leave.dto.LeaveBalanceAdjustmentResponse;
import com.hrms.leave.dto.LeaveBalanceResponse;
import com.hrms.leave.dto.LeaveTypeResponse;
import com.hrms.leave.service.LeaveBalanceService;
import com.hrms.leave.service.LeaveRolloverService;
import com.hrms.leave.service.LeaveTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/leave")
@RequiredArgsConstructor
@Tag(name = "Leave Module", description = "Leave types, balances, and admin operations")
public class LeaveModuleController {

    private final LeaveTypeService     leaveTypeService;
    private final LeaveBalanceService  leaveBalanceService;
    private final LeaveRolloverService leaveRolloverService;
    private final UserRepository       userRepository;

    // ── Leave Types ───────────────────────────────────────────────────────────

    @GetMapping("/types")
    @Operation(summary = "List all active leave types (accessible to all authenticated users)")
    public ResponseEntity<ApiResponse<List<LeaveTypeResponse>>> getActiveLeaveTypes() {
        return ResponseEntity.ok(ApiResponse.success(leaveTypeService.getAllActive()));
    }

    // ── Leave Balances ────────────────────────────────────────────────────────

    @GetMapping("/balances/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get the current user's leave balances for the current year")
    public ResponseEntity<ApiResponse<List<LeaveBalanceResponse>>> getMyBalances(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID employeeId = resolveEmployeeId(userDetails);
        int year = LocalDate.now().getYear();
        return ResponseEntity.ok(ApiResponse.success(leaveBalanceService.getBalances(employeeId, year)));
    }

    @GetMapping("/balances/{employeeId}")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'MANAGER')")
    @Operation(summary = "Get leave balances for a specific employee (HR Admin: anyone, Manager: own direct reports only)")
    public ResponseEntity<ApiResponse<List<LeaveBalanceResponse>>> getBalancesForEmployee(
            @PathVariable UUID employeeId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int year) {
        int targetYear = year > 0 ? year : LocalDate.now().getYear();
        boolean isHrAdmin = isHrAdmin(userDetails);
        UUID actingEmployeeId = isHrAdmin ? null : resolveEmployeeId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                leaveBalanceService.getBalancesScoped(employeeId, targetYear, actingEmployeeId, isHrAdmin)));
    }

    @PatchMapping("/balances/{employeeId}/adjust")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'MANAGER')")
    @Operation(summary = "Adjust an employee's leave balance by a signed number of days, with a required reason (HR Admin: anyone, Manager: own direct reports only)")
    public ResponseEntity<ApiResponse<LeaveBalanceResponse>> adjustBalance(
            @PathVariable UUID employeeId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AdjustLeaveBalanceRequest request) {
        boolean isHrAdmin = isHrAdmin(userDetails);
        // Always resolve the acting employee — the adjustment audit trail must record who actually
        // made it, never fall back to the target employee just because the caller has no link.
        UUID actingEmployeeId = resolveEmployeeId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Leave balance adjusted successfully",
                leaveBalanceService.adjustBalance(employeeId, actingEmployeeId, isHrAdmin, request)));
    }

    @GetMapping("/balances/{employeeId}/adjustments")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'MANAGER')")
    @Operation(summary = "Get the adjustment history for one employee/leave type/year")
    public ResponseEntity<ApiResponse<List<LeaveBalanceAdjustmentResponse>>> getAdjustmentHistory(
            @PathVariable UUID employeeId,
            @RequestParam UUID leaveTypeId,
            @RequestParam(defaultValue = "0") int year) {
        int targetYear = year > 0 ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(ApiResponse.success(
                leaveBalanceService.getAdjustmentHistory(employeeId, leaveTypeId, targetYear)));
    }

    // ── Admin Operations ──────────────────────────────────────────────────────

    @PostMapping("/admin/run-annual-rollover")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Manually trigger the annual leave rollover (HR Admin only)")
    public ResponseEntity<ApiResponse<String>> runAnnualRollover(
            @RequestParam(defaultValue = "0") int year) {
        int targetYear = year > 0 ? year : LocalDate.now().getYear();
        log.info("Manual annual leave rollover triggered by admin for year {}", targetYear);
        int count = leaveRolloverService.runRollover(targetYear);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Annual rollover for %d completed — %d employees processed", targetYear, count)));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private boolean isHrAdmin(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HR_ADMIN"));
    }

    private UUID resolveEmployeeId(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .map(User::getEmployeeId)
                .filter(id -> id != null)
                .orElseThrow(() -> new AppException(
                        "No employee record linked to this user account",
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "NO_EMPLOYEE_LINKED"));
    }
}
