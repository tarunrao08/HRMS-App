package com.hrms.attendance.controller;

import com.hrms.attendance.dto.ShiftRequest;
import com.hrms.attendance.dto.ShiftResponse;
import com.hrms.attendance.service.ShiftService;
import com.hrms.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService shiftService;

    @PostMapping
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<ApiResponse<ShiftResponse>> create(@Valid @RequestBody ShiftRequest request) {
        ShiftResponse response = shiftService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Shift created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShiftResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(shiftService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShiftResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(shiftService.getById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<ApiResponse<ShiftResponse>> update(@PathVariable UUID id,
                                                              @Valid @RequestBody ShiftRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Shift updated successfully", shiftService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        shiftService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Shift deleted successfully"));
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<ApiResponse<ShiftResponse>> toggleActive(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Shift status toggled", shiftService.toggleActive(id)));
    }
}
