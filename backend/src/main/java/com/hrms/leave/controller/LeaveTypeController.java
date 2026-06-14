package com.hrms.leave.controller;

import com.hrms.common.dto.ApiResponse;
import com.hrms.leave.dto.LeaveTypeRequest;
import com.hrms.leave.dto.LeaveTypeResponse;
import com.hrms.leave.service.LeaveTypeService;
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
@RequestMapping("/api/leave-types")
@RequiredArgsConstructor
@Tag(name = "Leave Types", description = "Manage leave type master data")
public class LeaveTypeController {

    private final LeaveTypeService leaveTypeService;

    @PostMapping
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Create a new leave type")
    public ResponseEntity<ApiResponse<LeaveTypeResponse>> create(@Valid @RequestBody LeaveTypeRequest request) {
        LeaveTypeResponse response = leaveTypeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Leave type created successfully", response));
    }

    @GetMapping
    @Operation(summary = "List all leave types")
    public ResponseEntity<ApiResponse<List<LeaveTypeResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(leaveTypeService.getAll()));
    }

    @GetMapping("/active")
    @Operation(summary = "List all active leave types")
    public ResponseEntity<ApiResponse<List<LeaveTypeResponse>>> getAllActive() {
        return ResponseEntity.ok(ApiResponse.success(leaveTypeService.getAllActive()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a leave type by ID")
    public ResponseEntity<ApiResponse<LeaveTypeResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(leaveTypeService.getById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Update a leave type")
    public ResponseEntity<ApiResponse<LeaveTypeResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody LeaveTypeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Leave type updated successfully", leaveTypeService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Delete a leave type")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        leaveTypeService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Leave type deleted successfully"));
    }
}
