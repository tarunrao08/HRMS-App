package com.hrms.onboarding.event;

import org.springframework.context.ApplicationEvent;

import java.util.UUID;

public class OnboardingCompletedEvent extends ApplicationEvent {

    private final UUID employeeId;
    private final UUID workflowId;

    public OnboardingCompletedEvent(Object source, UUID employeeId, UUID workflowId) {
        super(source);
        this.employeeId = employeeId;
        this.workflowId = workflowId;
    }

    public UUID getEmployeeId() { return employeeId; }
    public UUID getWorkflowId() { return workflowId; }
}
