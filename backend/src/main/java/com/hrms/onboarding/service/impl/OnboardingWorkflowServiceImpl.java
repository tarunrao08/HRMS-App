package com.hrms.onboarding.service.impl;

import com.hrms.common.dto.PageableResponse;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.exception.ValidationException;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.onboarding.dto.InitiateWorkflowRequest;
import com.hrms.onboarding.dto.OnboardingWorkflowResponse;
import com.hrms.onboarding.entity.OnboardingTask;
import com.hrms.onboarding.entity.OnboardingTaskDefinition;
import com.hrms.onboarding.entity.OnboardingTemplate;
import com.hrms.onboarding.entity.OnboardingWorkflow;
import com.hrms.onboarding.enums.TaskStatus;
import com.hrms.onboarding.enums.WorkflowStatus;
import com.hrms.onboarding.mapper.OnboardingMapper;
import com.hrms.onboarding.repository.OnboardingTaskDefinitionRepository;
import com.hrms.onboarding.repository.OnboardingTaskRepository;
import com.hrms.onboarding.repository.OnboardingTemplateRepository;
import com.hrms.onboarding.repository.OnboardingWorkflowRepository;
import com.hrms.onboarding.service.OnboardingWorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OnboardingWorkflowServiceImpl implements OnboardingWorkflowService {

    private final OnboardingWorkflowRepository workflowRepository;
    private final OnboardingTemplateRepository templateRepository;
    private final OnboardingTaskDefinitionRepository taskDefinitionRepository;
    private final OnboardingTaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final OnboardingMapper mapper;

    @Override
    public OnboardingWorkflowResponse initiate(InitiateWorkflowRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getEmployeeId().toString()));

        if (workflowRepository.findByEmployeeId(request.getEmployeeId()).isPresent()) {
            throw new ValidationException("Onboarding workflow already exists for employee: " + request.getEmployeeId());
        }

        OnboardingTemplate template;
        if (request.getTemplateId() != null) {
            template = templateRepository.findById(request.getTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("OnboardingTemplate", "id", request.getTemplateId().toString()));
        } else {
            template = templateRepository.findByDefaultTemplateTrue()
                    .orElseThrow(() -> new ValidationException("No default onboarding template configured"));
        }

        Employee assignedHr = null;
        if (request.getAssignedHrId() != null) {
            assignedHr = employeeRepository.findById(request.getAssignedHrId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getAssignedHrId().toString()));
        }

        List<OnboardingTaskDefinition> definitions = taskDefinitionRepository.findByTemplateIdOrderByStepNumberAsc(template.getId());

        OnboardingWorkflow workflow = OnboardingWorkflow.builder()
                .employee(employee)
                .template(template)
                .status(WorkflowStatus.NOT_STARTED)
                .currentStep(1)
                .totalSteps(definitions.size())
                .assignedHr(assignedHr)
                .build();

        workflow = workflowRepository.save(workflow);

        for (OnboardingTaskDefinition definition : definitions) {
            OnboardingTask task = OnboardingTask.builder()
                    .workflow(workflow)
                    .taskDefinition(definition)
                    .stepNumber(definition.getStepNumber())
                    .title(definition.getTitle())
                    .description(definition.getDescription())
                    .taskType(definition.getTaskType())
                    .status(TaskStatus.PENDING)
                    .dueDate(employee.getJoiningDate() != null
                            ? employee.getJoiningDate().plusDays(definition.getDueDaysFromJoining())
                            : null)
                    .build();
            taskRepository.save(task);
        }

        log.info("Initiated onboarding workflow for employee: {}", employee.getEmployeeCode());
        return mapper.toWorkflowResponse(workflow);
    }

    @Override
    public OnboardingWorkflowResponse initiateOnboarding(UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId.toString()));

        if (workflowRepository.findByEmployeeId(employeeId).isPresent()) {
            throw new ValidationException("Onboarding workflow already exists for employee: " + employeeId);
        }

        OnboardingTemplate template = templateRepository.findByDefaultTemplateTrue()
                .orElseThrow(() -> new ValidationException("No default onboarding template configured"));

        List<OnboardingTaskDefinition> definitions = taskDefinitionRepository
                .findByTemplateIdOrderByStepNumberAsc(template.getId());

        OnboardingWorkflow workflow = OnboardingWorkflow.builder()
                .employee(employee)
                .template(template)
                .status(WorkflowStatus.IN_PROGRESS)
                .currentStep(1)
                .totalSteps(definitions.size())
                .startedAt(Instant.now())
                .build();

        workflow = workflowRepository.save(workflow);

        for (OnboardingTaskDefinition definition : definitions) {
            OnboardingTask task = OnboardingTask.builder()
                    .workflow(workflow)
                    .taskDefinition(definition)
                    .stepNumber(definition.getStepNumber())
                    .title(definition.getTitle())
                    .description(definition.getDescription())
                    .taskType(definition.getTaskType())
                    .status(TaskStatus.PENDING)
                    .dueDate(employee.getJoiningDate() != null
                            ? employee.getJoiningDate().plusDays(definition.getDueDaysFromJoining())
                            : null)
                    .build();
            taskRepository.save(task);
        }

        log.info("Auto-initiated onboarding workflow for employee: {}", employee.getEmployeeCode());
        return mapper.toWorkflowResponse(workflow);
    }

    @Override
    @Transactional(readOnly = true)
    public OnboardingWorkflowResponse getByEmployee(UUID employeeId) {
        OnboardingWorkflow workflow = workflowRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("OnboardingWorkflow", "employeeId", employeeId.toString()));
        return mapper.toWorkflowResponse(workflow);
    }

    @Override
    @Transactional(readOnly = true)
    public OnboardingWorkflowResponse getById(UUID workflowId) {
        return mapper.toWorkflowResponse(findWorkflowOrThrow(workflowId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageableResponse<OnboardingWorkflowResponse> getByStatus(WorkflowStatus status, Pageable pageable) {
        Page<OnboardingWorkflowResponse> page = workflowRepository.findByStatus(status, pageable)
                .map(mapper::toWorkflowResponse);
        return PageableResponse.of(page);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OnboardingWorkflowResponse> getAll() {
        return workflowRepository.findAll().stream()
                .map(mapper::toWorkflowResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OnboardingWorkflowResponse> getAssignedToHr(UUID hrId) {
        return workflowRepository.findByAssignedHrId(hrId).stream()
                .map(mapper::toWorkflowResponse)
                .toList();
    }

    @Override
    public OnboardingWorkflowResponse start(UUID workflowId) {
        OnboardingWorkflow workflow = findWorkflowOrThrow(workflowId);
        if (workflow.getStatus() != WorkflowStatus.NOT_STARTED) {
            throw new ValidationException("Workflow cannot be started — current status: " + workflow.getStatus());
        }
        workflow.setStatus(WorkflowStatus.IN_PROGRESS);
        workflow.setStartedAt(Instant.now());
        workflow = workflowRepository.save(workflow);
        log.info("Started onboarding workflow: {}", workflowId);
        return mapper.toWorkflowResponse(workflow);
    }

    @Override
    public OnboardingWorkflowResponse complete(UUID workflowId) {
        OnboardingWorkflow workflow = findWorkflowOrThrow(workflowId);

        List<OnboardingTask> tasks = taskRepository.findByWorkflowIdOrderByStepNumberAsc(workflowId);
        boolean allMandatoryCompleted = tasks.stream()
                .filter(t -> t.getTaskDefinition().isMandatory())
                .allMatch(t -> t.getStatus() == TaskStatus.COMPLETED);

        if (!allMandatoryCompleted) {
            throw new ValidationException("Cannot complete workflow: not all mandatory tasks are completed");
        }

        workflow.setStatus(WorkflowStatus.COMPLETED);
        workflow.setCompletedAt(Instant.now());
        workflow = workflowRepository.save(workflow);
        log.info("Completed onboarding workflow: {}", workflowId);
        return mapper.toWorkflowResponse(workflow);
    }

    private OnboardingWorkflow findWorkflowOrThrow(UUID id) {
        return workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OnboardingWorkflow", "id", id.toString()));
    }
}
