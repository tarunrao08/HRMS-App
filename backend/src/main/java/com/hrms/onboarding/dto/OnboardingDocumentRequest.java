package com.hrms.onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class OnboardingDocumentRequest {

    @NotNull
    private UUID employeeId;

    private UUID taskId;

    @NotBlank
    private String documentType;

    @NotBlank
    private String documentName;

    @NotBlank
    private String fileUrl;

    private Long fileSizeBytes;

    private String mimeType;
}
