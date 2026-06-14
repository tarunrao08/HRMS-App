package com.hrms.onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class OnboardingTemplateRequest {

    @NotBlank
    private String name;

    private String description;

    private UUID departmentId;

    private UUID designationId;

    private boolean defaultTemplate;

    private boolean active = true;
}
