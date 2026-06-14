package com.hrms.payroll.controller;

import com.hrms.common.dto.ApiResponse;
import com.hrms.payroll.dto.PayrollComponentRequest;
import com.hrms.payroll.dto.PayrollComponentResponse;
import com.hrms.payroll.service.PayrollComponentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payroll/components")
@RequiredArgsConstructor
@Tag(name = "Payroll Components", description = "Manage payroll components (earnings, deductions, statutory)")
public class PayrollComponentController {

    private final PayrollComponentService payrollComponentService;

    @PostMapping
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Create a new payroll component")
    public ResponseEntity<ApiResponse<PayrollComponentResponse>> create(
            @Valid @RequestBody PayrollComponentRequest request) {
        PayrollComponentResponse response = payrollComponentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payroll component created successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Get all payroll components")
    public ResponseEntity<ApiResponse<List<PayrollComponentResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(payrollComponentService.getAll()));
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Get all active payroll components ordered by display order")
    public ResponseEntity<ApiResponse<List<PayrollComponentResponse>>> getActive() {
        return ResponseEntity.ok(ApiResponse.success(payrollComponentService.getActive()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Get a payroll component by ID")
    public ResponseEntity<ApiResponse<PayrollComponentResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(payrollComponentService.getById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Update a payroll component")
    public ResponseEntity<ApiResponse<PayrollComponentResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody PayrollComponentRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Payroll component updated successfully",
                        payrollComponentService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Delete a payroll component")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        payrollComponentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Payroll component deleted successfully"));
    }
}
