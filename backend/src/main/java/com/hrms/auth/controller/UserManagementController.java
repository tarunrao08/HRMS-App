package com.hrms.auth.controller;

import com.hrms.auth.dto.CreateUserRequest;
import com.hrms.auth.dto.ResetPasswordRequest;
import com.hrms.auth.dto.UserResponse;
import com.hrms.auth.service.UserManagementService;
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
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('HR_ADMIN')")
public class UserManagementController {

    private final UserManagementService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User account created successfully", userService.createUser(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(userService.getAll()));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<UserResponse>> resetPassword(
            @PathVariable UUID id,
            @Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully",
                userService.resetPassword(id, request.getNewPassword())));
    }
}
