package com.hrms.onboarding.dto;

import com.hrms.onboarding.enums.TaskStatus;
import com.hrms.onboarding.enums.TaskType;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class OnboardingTaskResponse {

    private UUID id;
    private UUID workflowId;
    private UUID taskDefinitionId;
    private int stepNumber;
    private String title;
    private String description;
    private TaskType taskType;
    private TaskStatus status;
    private LocalDate dueDate;
    private Instant completedAt;
    private UUID completedBy;
    private String notes;
    private String documentUrl;
    private String rejectionReason;
}
