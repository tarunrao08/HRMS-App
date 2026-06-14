package com.hrms.onboarding.service;

import com.hrms.onboarding.dto.OnboardingDocumentRequest;
import com.hrms.onboarding.dto.OnboardingDocumentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface OnboardingDocumentService {

    OnboardingDocumentResponse upload(OnboardingDocumentRequest request);

    /** Multipart upload — stores file locally and persists record linked to the workflow's employee. */
    OnboardingDocumentResponse uploadFile(UUID workflowId, UUID taskId, String documentType, MultipartFile file, UUID uploadedByUserId);

    List<OnboardingDocumentResponse> getByEmployee(UUID employeeId);

    List<OnboardingDocumentResponse> getByWorkflow(UUID workflowId);

    List<OnboardingDocumentResponse> getByTask(UUID taskId);

    OnboardingDocumentResponse verify(UUID documentId, UUID verifiedByUserId);

    OnboardingDocumentResponse reject(UUID documentId, String rejectionReason);
}
