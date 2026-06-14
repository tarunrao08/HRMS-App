package com.hrms.onboarding.mapper;

import com.hrms.onboarding.dto.*;
import com.hrms.onboarding.entity.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface OnboardingMapper {

    @Mapping(source = "department.id", target = "departmentId")
    @Mapping(source = "department.name", target = "departmentName")
    @Mapping(source = "designation.id", target = "designationId")
    @Mapping(source = "designation.name", target = "designationTitle")
    OnboardingTemplateResponse toTemplateResponse(OnboardingTemplate template);

    @Mapping(target = "department", ignore = true)
    @Mapping(target = "designation", ignore = true)
    OnboardingTemplate toTemplateEntity(OnboardingTemplateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "designation", ignore = true)
    void updateTemplate(@MappingTarget OnboardingTemplate template, OnboardingTemplateRequest request);

    @Mapping(source = "template.id", target = "templateId")
    OnboardingTaskDefinitionResponse toTaskDefinitionResponse(OnboardingTaskDefinition definition);

    @Mapping(target = "template", ignore = true)
    OnboardingTaskDefinition toTaskDefinitionEntity(OnboardingTaskDefinitionRequest request);

    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(expression = "java(workflow.getEmployee().getFirstName() + \" \" + workflow.getEmployee().getLastName())", target = "employeeName")
    @Mapping(source = "employee.employeeCode", target = "employeeCode")
    @Mapping(source = "template.id", target = "templateId")
    @Mapping(source = "template.name", target = "templateName")
    @Mapping(source = "assignedHr.id", target = "assignedHrId")
    @Mapping(expression = "java(workflow.getAssignedHr() != null ? workflow.getAssignedHr().getFirstName() + \" \" + workflow.getAssignedHr().getLastName() : null)", target = "assignedHrName")
    @Mapping(source = "totalSteps", target = "totalTasks")
    @Mapping(source = "employee.joiningDate", target = "startDate")
    @Mapping(expression = "java(workflow.getStatus() == com.hrms.onboarding.enums.WorkflowStatus.COMPLETED ? workflow.getTotalSteps() : Math.max(0, workflow.getCurrentStep() - 1))", target = "completedTasks")
    @Mapping(expression = "java(workflow.getStartedAt() != null ? java.time.temporal.ChronoUnit.DAYS.between(workflow.getStartedAt(), java.time.Instant.now()) : null)", target = "daysInProgress")
    OnboardingWorkflowResponse toWorkflowResponse(OnboardingWorkflow workflow);

    @Mapping(source = "workflow.id", target = "workflowId")
    @Mapping(source = "taskDefinition.id", target = "taskDefinitionId")
    OnboardingTaskResponse toTaskResponse(OnboardingTask task);

    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(expression = "java(document.getEmployee().getFirstName() + \" \" + document.getEmployee().getLastName())", target = "employeeName")
    @Mapping(source = "task.id", target = "taskId")
    OnboardingDocumentResponse toDocumentResponse(OnboardingDocument document);
}
