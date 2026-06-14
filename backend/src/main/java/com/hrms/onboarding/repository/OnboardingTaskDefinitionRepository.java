package com.hrms.onboarding.repository;

import com.hrms.onboarding.entity.OnboardingTaskDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OnboardingTaskDefinitionRepository extends JpaRepository<OnboardingTaskDefinition, UUID> {

    List<OnboardingTaskDefinition> findByTemplateIdOrderByStepNumberAsc(UUID templateId);
}
