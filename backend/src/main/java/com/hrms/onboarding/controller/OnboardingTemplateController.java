package com.hrms.onboarding.controller;

import com.hrms.common.dto.ApiResponse;
import com.hrms.onboarding.dto.*;
import com.hrms.onboarding.service.OnboardingTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/onboarding/templates")
@RequiredArgsConstructor
public class OnboardingTemplateController {

    private final OnboardingTemplateService templateService;

    @PostMapping
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<ApiResponse<OnboardingTemplateResponse>> create(
            @Valid @RequestBody OnboardingTemplateRequest request) {
        OnboardingTemplateResponse response = templateService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Onboarding template created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OnboardingTemplateResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(templateService.getAll()));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<OnboardingTemplateResponse>>> getAllActive() {
        return ResponseEntity.ok(ApiResponse.success(templateService.getAllActive()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OnboardingTemplateResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(templateService.getById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<ApiResponse<OnboardingTemplateResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody OnboardingTemplateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Onboarding template updated successfully", templateService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        templateService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Onboarding template deleted successfully"));
    }

    @PostMapping("/{id}/task-definitions")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<ApiResponse<OnboardingTaskDefinitionResponse>> addTaskDefinition(
            @PathVariable UUID id,
            @Valid @RequestBody OnboardingTaskDefinitionRequest request) {
        OnboardingTaskDefinitionResponse response = templateService.addTaskDefinition(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Task definition added successfully", response));
    }

    @GetMapping("/{id}/task-definitions")
    public ResponseEntity<ApiResponse<List<OnboardingTaskDefinitionResponse>>> getTaskDefinitions(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(templateService.getTaskDefinitions(id)));
    }

    @PutMapping("/task-definitions/{defId}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<ApiResponse<OnboardingTaskDefinitionResponse>> updateTaskDefinition(
            @PathVariable UUID defId,
            @Valid @RequestBody OnboardingTaskDefinitionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Task definition updated successfully", templateService.updateTaskDefinition(defId, request)));
    }

    @DeleteMapping("/task-definitions/{defId}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTaskDefinition(@PathVariable UUID defId) {
        templateService.deleteTaskDefinition(defId);
        return ResponseEntity.ok(ApiResponse.success("Task definition deleted successfully"));
    }
}
