package com.hrms.employee.controller;

import com.hrms.common.dto.ApiResponse;
import com.hrms.employee.dto.NomineeRequest;
import com.hrms.employee.dto.NomineeResponse;
import com.hrms.employee.service.EmployeeNomineeService;
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
@RequestMapping("/api/employees/{employeeId}/nominees")
@RequiredArgsConstructor
@Tag(name = "Employee Nominees", description = "Manage an employee's statutory nominee records (PF/gratuity/insurance)")
public class EmployeeNomineeController {

    private final EmployeeNomineeService nomineeService;

    @GetMapping
    @Operation(summary = "List nominees for an employee")
    public ResponseEntity<ApiResponse<List<NomineeResponse>>> list(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(nomineeService.list(employeeId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Add a nominee (total share across all nominees must not exceed 100)")
    public ResponseEntity<ApiResponse<NomineeResponse>> add(
            @PathVariable UUID employeeId, @Valid @RequestBody NomineeRequest request) {
        NomineeResponse response = nomineeService.add(employeeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Nominee added successfully", response));
    }

    @PutMapping
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Replace all nominees for an employee (shares must sum to exactly 100)")
    public ResponseEntity<ApiResponse<List<NomineeResponse>>> replaceAll(
            @PathVariable UUID employeeId, @Valid @RequestBody List<NomineeRequest> requests) {
        return ResponseEntity.ok(ApiResponse.success("Nominees updated successfully",
                nomineeService.replaceAll(employeeId, requests)));
    }

    @PutMapping("/{nomineeId}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Update a single nominee")
    public ResponseEntity<ApiResponse<NomineeResponse>> update(
            @PathVariable UUID employeeId, @PathVariable UUID nomineeId,
            @Valid @RequestBody NomineeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Nominee updated successfully",
                nomineeService.update(employeeId, nomineeId, request)));
    }

    @DeleteMapping("/{nomineeId}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Remove a nominee")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID employeeId, @PathVariable UUID nomineeId) {
        nomineeService.delete(employeeId, nomineeId);
        return ResponseEntity.ok(ApiResponse.success("Nominee removed successfully"));
    }
}
