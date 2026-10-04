package com.hrms.employee.controller;

import com.hrms.common.dto.ApiResponse;
import com.hrms.employee.dto.FamilyMemberRequest;
import com.hrms.employee.dto.FamilyMemberResponse;
import com.hrms.employee.service.EmployeeFamilyMemberService;
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
@RequestMapping("/api/employees/{employeeId}/family-members")
@RequiredArgsConstructor
@Tag(name = "Employee Family Members", description = "Manage an employee's family member records")
public class EmployeeFamilyMemberController {

    private final EmployeeFamilyMemberService familyMemberService;

    @GetMapping
    @Operation(summary = "List family members for an employee")
    public ResponseEntity<ApiResponse<List<FamilyMemberResponse>>> list(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(familyMemberService.list(employeeId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Add a family member")
    public ResponseEntity<ApiResponse<FamilyMemberResponse>> add(
            @PathVariable UUID employeeId, @Valid @RequestBody FamilyMemberRequest request) {
        FamilyMemberResponse response = familyMemberService.add(employeeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Family member added successfully", response));
    }

    @PutMapping
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Replace all family members for an employee")
    public ResponseEntity<ApiResponse<List<FamilyMemberResponse>>> replaceAll(
            @PathVariable UUID employeeId, @Valid @RequestBody List<FamilyMemberRequest> requests) {
        return ResponseEntity.ok(ApiResponse.success("Family members updated successfully",
                familyMemberService.replaceAll(employeeId, requests)));
    }

    @PutMapping("/{memberId}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Update a single family member")
    public ResponseEntity<ApiResponse<FamilyMemberResponse>> update(
            @PathVariable UUID employeeId, @PathVariable UUID memberId,
            @Valid @RequestBody FamilyMemberRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Family member updated successfully",
                familyMemberService.update(employeeId, memberId, request)));
    }

    @DeleteMapping("/{memberId}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Remove a family member")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID employeeId, @PathVariable UUID memberId) {
        familyMemberService.delete(employeeId, memberId);
        return ResponseEntity.ok(ApiResponse.success("Family member removed successfully"));
    }
}
