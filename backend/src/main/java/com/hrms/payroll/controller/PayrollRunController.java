package com.hrms.payroll.controller;

import com.hrms.auth.entity.User;
import com.hrms.auth.repository.UserRepository;
import com.hrms.common.dto.ApiResponse;
import com.hrms.common.dto.PageableResponse;
import com.hrms.payroll.dto.InitiatePayrollRunRequest;
import com.hrms.payroll.dto.PayrollRunResponse;
import com.hrms.payroll.dto.PayslipResponse;
import com.hrms.payroll.service.PayrollRunService;
import com.hrms.payroll.service.PayslipService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payroll/runs")
@RequiredArgsConstructor
@Tag(name = "Payroll Runs", description = "Manage payroll run lifecycle")
public class PayrollRunController {

    private final PayrollRunService payrollRunService;
    private final PayslipService payslipService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Initiate a new payroll run")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> initiate(
            @Valid @RequestBody InitiatePayrollRunRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID processedBy = resolveUserId(userDetails);
        PayrollRunResponse response = payrollRunService.initiate(request, processedBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payroll run initiated successfully", response));
    }

    @PostMapping("/{id}/process")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Process a payroll run (generate payslips)")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> process(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.success("Payroll run processed successfully", payrollRunService.process(id)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Approve a processed payroll run")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> approve(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID approvedBy = resolveUserId(userDetails);
        return ResponseEntity.ok(
                ApiResponse.success("Payroll run approved successfully",
                        payrollRunService.approve(id, approvedBy)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Reject a processed payroll run and move it back to DRAFT for re-processing")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> reject(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.success("Payroll run rejected and moved back to DRAFT",
                        payrollRunService.reject(id)));
    }

    @PostMapping("/{id}/disburse")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Mark an approved payroll run as disbursed (salaries paid)")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> disburse(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID disbursedBy = resolveUserId(userDetails);
        return ResponseEntity.ok(
                ApiResponse.success("Payroll run marked as disbursed",
                        payrollRunService.disburse(id, disbursedBy)));
    }

    @GetMapping
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Get all payroll runs paginated")
    public ResponseEntity<ApiResponse<PageableResponse<PayrollRunResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "year") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(ApiResponse.paginated(payrollRunService.getAll(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Get a payroll run by ID")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(payrollRunService.getById(id)));
    }

    @GetMapping("/{id}/payslips")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Get all payslips for a payroll run")
    public ResponseEntity<ApiResponse<List<PayslipResponse>>> getPayslips(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(payslipService.getByRun(id)));
    }

    private UUID resolveUserId(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .map(User::getId)
                .orElse(null);
    }
}
