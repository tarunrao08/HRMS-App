package com.hrms.payroll.controller;

import com.hrms.auth.entity.User;
import com.hrms.auth.repository.UserRepository;
import com.hrms.common.dto.ApiResponse;
import com.hrms.payroll.dto.TdsDeclarationRequest;
import com.hrms.payroll.dto.TdsDeclarationResponse;
import com.hrms.payroll.service.TdsDeclarationService;
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
@RequestMapping("/api/payroll/tds-declarations")
@RequiredArgsConstructor
@Tag(name = "TDS Declarations", description = "Manage employee TDS/tax declarations")
public class TdsDeclarationController {

    private final TdsDeclarationService tdsDeclarationService;
    private final UserRepository        userRepository;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit a TDS declaration")
    public ResponseEntity<ApiResponse<TdsDeclarationResponse>> create(
            @Valid @RequestBody TdsDeclarationRequest request) {
        TdsDeclarationResponse response = tdsDeclarationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("TDS declaration submitted successfully", response));
    }

    @GetMapping("/my/{financialYear}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get the current user's own TDS declaration for a financial year")
    public ResponseEntity<ApiResponse<TdsDeclarationResponse>> getMyDeclaration(
            @PathVariable String financialYear,
            Authentication authentication) {
        UUID employeeId = userRepository.findByUsername(authentication.getName())
                .map(User::getEmployeeId).orElse(null);
        if (employeeId == null) return ResponseEntity.notFound().build();
        return tdsDeclarationService.findMyDeclaration(employeeId, financialYear)
                .map(d -> ResponseEntity.ok(ApiResponse.success(d)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/employee/{id}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Get all TDS declarations for an employee")
    public ResponseEntity<ApiResponse<List<TdsDeclarationResponse>>> getByEmployee(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(tdsDeclarationService.getByEmployee(id)));
    }

    @GetMapping("/employee/{id}/{financialYear}")
    @Operation(summary = "Get a TDS declaration for an employee and financial year")
    public ResponseEntity<ApiResponse<TdsDeclarationResponse>> getByEmployeeAndYear(
            @PathVariable UUID id,
            @PathVariable String financialYear) {
        return ResponseEntity.ok(ApiResponse.success(
                tdsDeclarationService.getByEmployeeAndYear(id, financialYear)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update a TDS declaration")
    public ResponseEntity<ApiResponse<TdsDeclarationResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody TdsDeclarationRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("TDS declaration updated successfully",
                        tdsDeclarationService.update(id, request)));
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Verify a TDS declaration")
    public ResponseEntity<ApiResponse<TdsDeclarationResponse>> verify(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.success("TDS declaration verified successfully",
                        tdsDeclarationService.verify(id)));
    }
}
