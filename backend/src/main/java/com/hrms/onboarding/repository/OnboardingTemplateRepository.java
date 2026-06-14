package com.hrms.onboarding.repository;

import com.hrms.onboarding.entity.OnboardingTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OnboardingTemplateRepository extends JpaRepository<OnboardingTemplate, UUID> {

    Optional<OnboardingTemplate> findByDefaultTemplateTrue();

    boolean existsByNameIgnoreCase(String name);

    List<OnboardingTemplate> findByActiveTrueOrderByNameAsc();
}
