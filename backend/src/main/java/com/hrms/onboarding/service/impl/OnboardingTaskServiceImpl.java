package com.hrms.onboarding.service.impl;

import com.hrms.common.exception.AppException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.exception.ValidationException;
import com.hrms.onboarding.dto.OnboardingTaskResponse;
import com.hrms.onboarding.dto.UpdateTaskRequest;
import com.hrms.onboarding.entity.OnboardingTask;
import com.hrms.onboarding.entity.OnboardingWorkflow;
import com.hrms.onboarding.enums.TaskStatus;
import com.hrms.onboarding.enums.WorkflowStatus;
import com.hrms.onboarding.event.OnboardingCompletedEvent;
import com.hrms.onboarding.mapper.OnboardingMapper;
import com.hrms.onboarding.repository.OnboardingTaskRepository;
import com.hrms.onboarding.repository.OnboardingWorkflowRepository;
import com.hrms.onboarding.service.OnboardingTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OnboardingTaskServiceImpl implements OnboardingTaskService {

    private final OnboardingTaskRepository     taskRepository;
    private final OnboardingWorkflowRepository workflowRepository;
    private final OnboardingMapper             mapper;
    private final ApplicationEventPublisher    eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<OnboardingTaskResponse> getTasksByWorkflow(UUID workflowId) {
        return taskRepository.findByWorkflowIdOrderByStepNumberAsc(workflowId).stream()
                .map(mapper::toTaskResponse)
                .toList();
    }

    /**
     * Generic task update used by the existing PATCH /workflows/tasks/{taskId} endpoint.
     * When the new status is COMPLETED the step-completion state machine runs automatically.
     */
    @Override
    public OnboardingTaskResponse updateTask(UUID taskId, UUID completedByUserId, UpdateTaskRequest request) {
        OnboardingTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("OnboardingTask", "id", taskId.toString()));

        if (request.getStatus() == TaskStatus.COMPLETED) {
            checkRolePermission(task);
        }

        task.setStatus(request.getStatus());
        task.setNotes(request.getNotes());
        task.setDocumentUrl(request.getDocumentUrl());
        task.setRejectionReason(request.getRejectionReason());

        if (request.getStatus() == TaskStatus.COMPLETED) {
            task.setCompletedAt(Instant.now());
            task.setCompletedBy(completedByUserId);
        }

        task = taskRepository.save(task);

        if (request.getStatus() == TaskStatus.COMPLETED) {
            advanceWorkflowIfStepComplete(task.getWorkflow().getId());
        }

        log.info("Updated onboarding task {} to status {}", taskId, request.getStatus());
        return mapper.toTaskResponse(task);
    }

    /**
     * Explicit complete endpoint — validates the task belongs to the workflow,
     * enforces role rules, runs the state machine.
     */
    @Override
    public OnboardingTaskResponse completeTask(UUID workflowId, UUID taskId,
                                               UUID completedByUserId, String notes) {
        OnboardingTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("OnboardingTask", "id", taskId.toString()));

        if (!task.getWorkflow().getId().equals(workflowId)) {
            throw new ValidationException(
                    "Task " + taskId + " does not belong to workflow " + workflowId);
        }

        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new ValidationException("Task is already completed");
        }

        checkRolePermission(task);

        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(Instant.now());
        task.setCompletedBy(completedByUserId);
        if (notes != null && !notes.isBlank()) {
            task.setNotes(notes);
        }

        task = taskRepository.save(task);
        advanceWorkflowIfStepComplete(workflowId);

        log.info("Completed onboarding task {} in workflow {}", taskId, workflowId);
        return mapper.toTaskResponse(task);
    }

    // ─── State machine ────────────────────────────────────────────────────────

    /**
     * After a task is saved as COMPLETED, check whether all mandatory tasks for
     * the workflow's current step are done.
     *
     * <ul>
     *   <li>If yes and there are more steps: advance currentStep by 1.</li>
     *   <li>If yes and this was the final step: set status=COMPLETED and publish
     *       OnboardingCompletedEvent.</li>
     *   <li>If no: do nothing — the step is still in progress.</li>
     * </ul>
     */
    private void advanceWorkflowIfStepComplete(UUID workflowId) {
        OnboardingWorkflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("OnboardingWorkflow", "id", workflowId.toString()));

        if (workflow.getStatus() == WorkflowStatus.COMPLETED) {
            return; // already done, nothing to advance
        }

        int currentStep = workflow.getCurrentStep();

        // Fetch all tasks with their definitions in one query (avoids N+1)
        List<OnboardingTask> allTasks = taskRepository.findWithDefinitionsByWorkflowId(workflowId);

        boolean currentStepMandatoryAllDone = allTasks.stream()
                .filter(t -> t.getStepNumber() == currentStep && t.getTaskDefinition().isMandatory())
                .allMatch(t -> t.getStatus() == TaskStatus.COMPLETED);

        if (!currentStepMandatoryAllDone) {
            return; // step not yet finished
        }

        if (currentStep >= workflow.getTotalSteps()) {
            // Final step complete → complete the workflow
            workflow.setStatus(WorkflowStatus.COMPLETED);
            workflow.setCompletedAt(Instant.now());
            workflowRepository.save(workflow);
            eventPublisher.publishEvent(
                    new OnboardingCompletedEvent(this, workflow.getEmployee().getId(), workflow.getId()));
            log.info("Onboarding workflow {} completed for employee {}",
                    workflowId, workflow.getEmployee().getId());
        } else {
            // Advance to next step
            workflow.setCurrentStep(currentStep + 1);
            workflowRepository.save(workflow);
            log.info("Onboarding workflow {} advanced from step {} to step {}",
                    workflowId, currentStep, currentStep + 1);
        }
    }

    // ─── Role enforcement ─────────────────────────────────────────────────────

    /**
     * Enforces responsible_role from the task definition:
     * <ul>
     *   <li>ROLE_HR_ADMIN → only HR Admin may complete it.</li>
     *   <li>ROLE_EMPLOYEE → any authenticated user may complete it (HR Admin and
     *       Manager can always act on behalf of an employee).</li>
     * </ul>
     */
    private void checkRolePermission(OnboardingTask task) {
        String requiredRole = task.getTaskDefinition().getResponsibleRole();
        if (requiredRole == null || !requiredRole.equals("ROLE_HR_ADMIN")) {
            return; // ROLE_EMPLOYEE tasks are open to everyone authenticated
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return;

        Set<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        if (!roles.contains("ROLE_HR_ADMIN")) {
            throw new AppException(
                    "Step " + task.getStepNumber() + " (" + task.getTitle() +
                    ") can only be completed by HR Admin",
                    HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }
    }
}
