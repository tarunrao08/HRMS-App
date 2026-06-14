package com.hrms.onboarding.service;

import com.hrms.onboarding.dto.*;

import java.util.List;
import java.util.UUID;

public interface OnboardingTemplateService {

    OnboardingTemplateResponse create(OnboardingTemplateRequest request);

    OnboardingTemplateResponse getById(UUID id);

    List<OnboardingTemplateResponse> getAll();

    List<OnboardingTemplateResponse> getAllActive();

    OnboardingTemplateResponse update(UUID id, OnboardingTemplateRequest request);

    void delete(UUID id);

    OnboardingTaskDefinitionResponse addTaskDefinition(UUID templateId, OnboardingTaskDefinitionRequest request);

    OnboardingTaskDefinitionResponse updateTaskDefinition(UUID defId, OnboardingTaskDefinitionRequest request);

    void deleteTaskDefinition(UUID defId);

    List<OnboardingTaskDefinitionResponse> getTaskDefinitions(UUID templateId);
}
