package com.hrms.onboarding.dto;

import com.hrms.onboarding.enums.TaskType;
import lombok.Data;

import java.util.UUID;

@Data
public class OnboardingTaskDefinitionResponse {

    private UUID id;
    private UUID templateId;
    private int stepNumber;
    private String title;
    private String description;
    private TaskType taskType;
    private int dueDaysFromJoining;
    private boolean mandatory;
    private String responsibleRole;
}
