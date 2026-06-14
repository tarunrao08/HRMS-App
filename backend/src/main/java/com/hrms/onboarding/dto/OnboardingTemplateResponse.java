package com.hrms.onboarding.dto;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class OnboardingTemplateResponse {

    private UUID id;
    private String name;
    private String description;
    private UUID departmentId;
    private String departmentName;
    private UUID designationId;
    private String designationTitle;
    private boolean defaultTemplate;
    private boolean active;
    private Instant createdAt;
}
