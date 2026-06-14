package com.hrms.onboarding.dto;

import com.hrms.onboarding.enums.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.UUID;

@Data
public class OnboardingTaskDefinitionRequest {

    @NotNull
    private UUID templateId;

    @NotNull
    @Positive
    private Integer stepNumber;

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private TaskType taskType;

    private int dueDaysFromJoining = 7;

    private boolean mandatory = true;

    private String responsibleRole;
}
