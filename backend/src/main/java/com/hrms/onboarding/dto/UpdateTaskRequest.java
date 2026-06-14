package com.hrms.onboarding.dto;

import com.hrms.onboarding.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateTaskRequest {

    @NotNull
    private TaskStatus status;

    private String notes;

    private String documentUrl;

    private String rejectionReason;
}
