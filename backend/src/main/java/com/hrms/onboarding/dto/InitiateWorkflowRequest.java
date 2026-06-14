package com.hrms.onboarding.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class InitiateWorkflowRequest {

    @NotNull
    private UUID employeeId;

    private UUID templateId;

    private UUID assignedHrId;
}
