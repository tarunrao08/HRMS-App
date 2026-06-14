package com.hrms.onboarding.dto;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class OnboardingDocumentResponse {

    private UUID id;
    private UUID employeeId;
    private String employeeName;
    private UUID taskId;
    private String documentType;
    private String documentName;
    private String fileUrl;
    private Long fileSizeBytes;
    private String mimeType;
    private Instant uploadedAt;
    private boolean verified;
    private UUID verifiedBy;
    private Instant verifiedAt;
    private String rejectionReason;
}
