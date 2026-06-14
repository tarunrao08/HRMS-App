package com.hrms.onboarding.controller;

import com.hrms.common.dto.ApiResponse;
import com.hrms.onboarding.dto.OnboardingDocumentRequest;
import com.hrms.onboarding.dto.OnboardingDocumentResponse;
import com.hrms.onboarding.service.OnboardingDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class OnboardingDocumentController {

    private final OnboardingDocumentService documentService;

    // ─── Workflow-scoped upload + list ────────────────────────────────────────

    @PostMapping(value = "/api/onboarding/workflows/{workflowId}/documents",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OnboardingDocumentResponse>> uploadFile(
            @PathVariable UUID workflowId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            @RequestParam(value = "taskId", required = false) UUID taskId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = resolveUserId(userDetails);
        OnboardingDocumentResponse response = documentService.uploadFile(workflowId, taskId, documentType, file, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Document uploaded successfully", response));
    }

    @GetMapping("/api/onboarding/workflows/{workflowId}/documents")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<OnboardingDocumentResponse>>> getByWorkflow(
            @PathVariable UUID workflowId) {
        return ResponseEntity.ok(ApiResponse.success(documentService.getByWorkflow(workflowId)));
    }

    // ─── Legacy JSON upload ────────────────────────────────────────────────────

    @PostMapping("/api/onboarding/documents/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OnboardingDocumentResponse>> upload(
            @Valid @RequestBody OnboardingDocumentRequest request) {
        OnboardingDocumentResponse response = documentService.upload(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Document uploaded successfully", response));
    }

    @GetMapping("/api/onboarding/documents/employee/{employeeId}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<ApiResponse<List<OnboardingDocumentResponse>>> getByEmployee(
            @PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(documentService.getByEmployee(employeeId)));
    }

    @GetMapping("/api/onboarding/documents/task/{taskId}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<ApiResponse<List<OnboardingDocumentResponse>>> getByTask(
            @PathVariable UUID taskId) {
        return ResponseEntity.ok(ApiResponse.success(documentService.getByTask(taskId)));
    }

    @PostMapping("/api/onboarding/documents/{id}/verify")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<ApiResponse<OnboardingDocumentResponse>> verify(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID verifiedByUserId = resolveUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Document verified successfully", documentService.verify(id, verifiedByUserId)));
    }

    @PostMapping("/api/onboarding/documents/{id}/reject")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<ApiResponse<OnboardingDocumentResponse>> reject(
            @PathVariable UUID id,
            @RequestParam String rejectionReason) {
        return ResponseEntity.ok(ApiResponse.success("Document rejected", documentService.reject(id, rejectionReason)));
    }

    private UUID resolveUserId(UserDetails userDetails) {
        if (userDetails == null) return null;
        try {
            return UUID.fromString(userDetails.getUsername());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
