package com.hrms.onboarding.service;

import com.hrms.common.dto.PageableResponse;
import com.hrms.onboarding.dto.InitiateWorkflowRequest;
import com.hrms.onboarding.dto.OnboardingWorkflowResponse;
import com.hrms.onboarding.enums.WorkflowStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface OnboardingWorkflowService {

    OnboardingWorkflowResponse initiate(InitiateWorkflowRequest request);

    /** Auto-triggered by EmployeeCreatedEvent: uses default template, starts immediately. */
    OnboardingWorkflowResponse initiateOnboarding(UUID employeeId);

    OnboardingWorkflowResponse getByEmployee(UUID employeeId);

    OnboardingWorkflowResponse getById(UUID workflowId);

    PageableResponse<OnboardingWorkflowResponse> getByStatus(WorkflowStatus status, Pageable pageable);

    List<OnboardingWorkflowResponse> getAll();

    List<OnboardingWorkflowResponse> getAssignedToHr(UUID hrId);

    OnboardingWorkflowResponse start(UUID workflowId);

    OnboardingWorkflowResponse complete(UUID workflowId);
}
