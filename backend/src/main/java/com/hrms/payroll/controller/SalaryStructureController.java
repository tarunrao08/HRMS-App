package com.hrms.payroll.controller;

import com.hrms.auth.entity.User;
import com.hrms.auth.repository.UserRepository;
import com.hrms.common.dto.ApiResponse;
import com.hrms.payroll.dto.PayableSummaryResponse;
import com.hrms.payroll.dto.SalaryStructureRequest;
import com.hrms.payroll.dto.SalaryStructureResponse;
import com.hrms.payroll.service.SalaryStructureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payroll/salary-structures")
@RequiredArgsConstructor
@Tag(name = "Salary Structures", description = "Manage employee salary structures")
public class SalaryStructureController {

    private final SalaryStructureService salaryStructureService;
    private final UserRepository         userRepository;

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get the current user's own active salary structure")
    public ResponseEntity<ApiResponse<SalaryStructureResponse>> getMySalaryStructure(
            Authentication authentication) {
        UUID employeeId = userRepository.findByUsername(authentication.getName())
                .map(User::getEmployeeId).orElse(null);
        if (employeeId == null) return ResponseEntity.notFound().build();
        return salaryStructureService.getMyActiveSalaryStructure(employeeId)
                .map(s -> ResponseEntity.ok(ApiResponse.success(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Get total monthly payable across all active employees")
    public ResponseEntity<ApiResponse<PayableSummaryResponse>> getTotalPayableSummary() {
        return ResponseEntity.ok(ApiResponse.success(salaryStructureService.getTotalPayableSummary()));
    }

    @PostMapping
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Create a salary structure for an employee")
    public ResponseEntity<ApiResponse<SalaryStructureResponse>> create(
            @Valid @RequestBody SalaryStructureRequest request) {
        SalaryStructureResponse response = salaryStructureService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Salary structure created successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Get a salary structure by ID")
    public ResponseEntity<ApiResponse<SalaryStructureResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(salaryStructureService.getById(id)));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Get all salary structures for an employee")
    public ResponseEntity<ApiResponse<List<SalaryStructureResponse>>> getByEmployee(
            @PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(salaryStructureService.getByEmployee(employeeId)));
    }

    @GetMapping("/employee/{employeeId}/active")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Get the current active salary structure for an employee")
    public ResponseEntity<ApiResponse<SalaryStructureResponse>> getActiveForEmployee(
            @PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(salaryStructureService.getActiveForEmployee(employeeId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Update a salary structure")
    public ResponseEntity<ApiResponse<SalaryStructureResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody SalaryStructureRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Salary structure updated successfully",
                        salaryStructureService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Delete a salary structure")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        salaryStructureService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Salary structure deleted successfully"));
    }
}
