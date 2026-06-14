package com.hrms.employee.controller;

import com.hrms.common.dto.ApiResponse;
import com.hrms.employee.dto.DesignationRequest;
import com.hrms.employee.dto.DesignationResponse;
import com.hrms.employee.service.DesignationService;
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
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/designations")
@RequiredArgsConstructor
@Tag(name = "Designations", description = "Manage job designations")
public class DesignationController {

    private final DesignationService designationService;

    @PostMapping
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Create a new designation")
    public ResponseEntity<ApiResponse<DesignationResponse>> create(@Valid @RequestBody DesignationRequest request) {
        DesignationResponse response = designationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Designation created successfully", response));
    }

    @GetMapping
    @Operation(summary = "List all designations, optionally filtered by department")
    public ResponseEntity<ApiResponse<List<DesignationResponse>>> getAll(
            @RequestParam(required = false) UUID departmentId) {
        return ResponseEntity.ok(ApiResponse.success(designationService.getAll(departmentId)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a designation by ID")
    public ResponseEntity<ApiResponse<DesignationResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(designationService.getById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Update a designation")
    public ResponseEntity<ApiResponse<DesignationResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody DesignationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Designation updated successfully", designationService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Delete a designation")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        designationService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Designation deleted successfully"));
    }
}
