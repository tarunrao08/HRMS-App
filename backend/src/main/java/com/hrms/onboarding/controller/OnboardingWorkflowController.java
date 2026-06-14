package com.hrms.onboarding.controller;

import com.hrms.common.dto.ApiResponse;
import com.hrms.common.dto.PageableResponse;
import com.hrms.onboarding.dto.InitiateWorkflowRequest;
import com.hrms.onboarding.dto.OnboardingTaskResponse;
import com.hrms.onboarding.dto.OnboardingWorkflowResponse;
import com.hrms.onboarding.dto.UpdateTaskRequest;
import com.hrms.onboarding.enums.WorkflowStatus;
import com.hrms.onboarding.service.OnboardingTaskService;
import com.hrms.onboarding.service.OnboardingWorkflowService;
import jakarta.validation.Valid;
import lombok.Data;
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
@RequestMapping("/api/onboarding/workflows")
@RequiredArgsConstructor
public class OnboardingWorkflowController {

    private final OnboardingWorkflowService workflowService;
    private final OnboardingTaskService     taskService;

    // ─── Workflow CRUD ────────────────────────────────────────────────────────

    @PostMapping("/initiate")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<ApiResponse<OnboardingWorkflowResponse>> initiate(
            @Valid @RequestBody InitiateWorkflowRequest request) {
        OnboardingWorkflowResponse response = workflowService.initiate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Onboarding workflow initiated successfully", response));
    }

    /** POST /api/onboarding/workflows — alias for /initiate, used by the frontend. */
    @PostMapping
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<ApiResponse<OnboardingWorkflowResponse>> initiatePost(
            @Valid @RequestBody InitiateWorkflowRequest request) {
        OnboardingWorkflowResponse response = workflowService.initiate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Onboarding workflow initiated successfully", response));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<OnboardingWorkflowResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(workflowService.getAll()));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<ApiResponse<OnboardingWorkflowResponse>> getByEmployee(
            @PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(workflowService.getByEmployee(employeeId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OnboardingWorkflowResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(workflowService.getById(id)));
    }

    @GetMapping
    public ResponseEntity<?> getByStatus(
            @RequestParam(required = false) WorkflowStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        if (status == null) {
            return ResponseEntity.ok(ApiResponse.success(workflowService.getAll()));
        }
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(ApiResponse.paginated(workflowService.getByStatus(status, pageable)));
    }

    @GetMapping("/assigned-hr/{hrId}")
    public ResponseEntity<ApiResponse<List<OnboardingWorkflowResponse>>> getAssignedToHr(
            @PathVariable UUID hrId) {
        return ResponseEntity.ok(ApiResponse.success(workflowService.getAssignedToHr(hrId)));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<ApiResponse<OnboardingWorkflowResponse>> start(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Workflow started successfully", workflowService.start(id)));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<ApiResponse<OnboardingWorkflowResponse>> complete(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Workflow completed successfully", workflowService.complete(id)));
    }

    // ─── Task operations ──────────────────────────────────────────────────────

    @GetMapping("/{id}/tasks")
    public ResponseEntity<ApiResponse<List<OnboardingTaskResponse>>> getTasks(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getTasksByWorkflow(id)));
    }

    /**
     * Generic task update — kept for backward compatibility with the existing frontend call.
     * State machine runs automatically when status == COMPLETED.
     */
    @PatchMapping("/tasks/{taskId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OnboardingTaskResponse>> updateTask(
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Task updated successfully",
                taskService.updateTask(taskId, resolveUserId(userDetails), request)));
    }

    /**
     * Explicit complete endpoint per the design spec.
     * Role enforcement (HR_ADMIN for IT Setup step) is checked in the service.
     */
    @PatchMapping("/{workflowId}/tasks/{taskId}/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OnboardingTaskResponse>> completeTask(
            @PathVariable UUID workflowId,
            @PathVariable UUID taskId,
            @RequestBody(required = false) CompleteTaskRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String notes = request != null ? request.getNotes() : null;
        return ResponseEntity.ok(ApiResponse.success("Task completed successfully",
                taskService.completeTask(workflowId, taskId, resolveUserId(userDetails), notes)));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private UUID resolveUserId(UserDetails userDetails) {
        if (userDetails == null) return null;
        try {
            return UUID.fromString(userDetails.getUsername());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /** Inline DTO — only used by the completeTask endpoint. */
    @Data
    public static class CompleteTaskRequest {
        private String notes;
    }
}
