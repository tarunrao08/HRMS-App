package com.hrms.onboarding.repository;

import com.hrms.onboarding.entity.OnboardingWorkflow;
import com.hrms.onboarding.enums.WorkflowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OnboardingWorkflowRepository extends JpaRepository<OnboardingWorkflow, UUID> {

    Optional<OnboardingWorkflow> findByEmployeeId(UUID employeeId);

    Page<OnboardingWorkflow> findByStatus(WorkflowStatus status, Pageable pageable);

    List<OnboardingWorkflow> findByAssignedHrId(UUID hrId);
}
