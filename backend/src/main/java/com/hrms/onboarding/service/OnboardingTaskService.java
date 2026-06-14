package com.hrms.onboarding.service;

import com.hrms.onboarding.dto.OnboardingTaskResponse;
import com.hrms.onboarding.dto.UpdateTaskRequest;

import java.util.List;
import java.util.UUID;

public interface OnboardingTaskService {

    List<OnboardingTaskResponse> getTasksByWorkflow(UUID workflowId);

    OnboardingTaskResponse updateTask(UUID taskId, UUID completedByUserId, UpdateTaskRequest request);

    /** Explicitly completes a task via the /{workflowId}/tasks/{taskId}/complete endpoint. */
    OnboardingTaskResponse completeTask(UUID workflowId, UUID taskId, UUID completedByUserId, String notes);
}
