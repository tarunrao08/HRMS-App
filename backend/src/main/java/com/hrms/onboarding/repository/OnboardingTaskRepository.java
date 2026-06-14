package com.hrms.onboarding.repository;

import com.hrms.onboarding.entity.OnboardingTask;
import com.hrms.onboarding.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OnboardingTaskRepository extends JpaRepository<OnboardingTask, UUID> {

    List<OnboardingTask> findByWorkflowIdOrderByStepNumberAsc(UUID workflowId);

    List<OnboardingTask> findByWorkflowIdAndStatus(UUID workflowId, TaskStatus status);

    /** Eager-fetches taskDefinition to avoid N+1 in the step-completion state machine. */
    @Query("SELECT t FROM OnboardingTask t JOIN FETCH t.taskDefinition WHERE t.workflow.id = :workflowId ORDER BY t.stepNumber ASC")
    List<OnboardingTask> findWithDefinitionsByWorkflowId(@Param("workflowId") UUID workflowId);
}
