package com.hrms.onboarding.dto;

import com.hrms.onboarding.enums.WorkflowStatus;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class OnboardingWorkflowResponse {

    private UUID id;
    private UUID employeeId;
    private String employeeName;
    private String employeeCode;
    private UUID templateId;
    private String templateName;
    private WorkflowStatus status;
    private int currentStep;
    private int totalSteps;
    private int completedTasks;
    private int totalTasks;
    private Long daysInProgress;
    private LocalDate startDate;
    private Instant startedAt;
    private Instant completedAt;
    private UUID assignedHrId;
    private String assignedHrName;
    private Instant createdAt;
}
