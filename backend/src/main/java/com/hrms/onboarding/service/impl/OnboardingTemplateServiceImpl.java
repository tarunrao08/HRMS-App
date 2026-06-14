package com.hrms.onboarding.service.impl;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.exception.ValidationException;
import com.hrms.employee.repository.DepartmentRepository;
import com.hrms.employee.repository.DesignationRepository;
import com.hrms.onboarding.dto.*;
import com.hrms.onboarding.entity.OnboardingTaskDefinition;
import com.hrms.onboarding.entity.OnboardingTemplate;
import com.hrms.onboarding.mapper.OnboardingMapper;
import com.hrms.onboarding.repository.OnboardingTaskDefinitionRepository;
import com.hrms.onboarding.repository.OnboardingTemplateRepository;
import com.hrms.onboarding.service.OnboardingTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OnboardingTemplateServiceImpl implements OnboardingTemplateService {

    private final OnboardingTemplateRepository templateRepository;
    private final OnboardingTaskDefinitionRepository taskDefinitionRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final OnboardingMapper mapper;

    @Override
    public OnboardingTemplateResponse create(OnboardingTemplateRequest request) {
        if (templateRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ValidationException("Onboarding template with name '" + request.getName() + "' already exists");
        }
        if (request.isDefaultTemplate()) {
            clearDefaultFlag();
        }
        OnboardingTemplate template = mapper.toTemplateEntity(request);
        resolveAssociations(template, request);
        template = templateRepository.save(template);
        log.info("Created onboarding template: {}", template.getName());
        return mapper.toTemplateResponse(template);
    }

    @Override
    @Transactional(readOnly = true)
    public OnboardingTemplateResponse getById(UUID id) {
        return mapper.toTemplateResponse(findTemplateOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OnboardingTemplateResponse> getAll() {
        return templateRepository.findAll().stream()
                .map(mapper::toTemplateResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OnboardingTemplateResponse> getAllActive() {
        return templateRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(mapper::toTemplateResponse)
                .toList();
    }

    @Override
    public OnboardingTemplateResponse update(UUID id, OnboardingTemplateRequest request) {
        OnboardingTemplate template = findTemplateOrThrow(id);
        boolean nameChanged = !template.getName().equalsIgnoreCase(request.getName());
        if (nameChanged && templateRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ValidationException("Onboarding template with name '" + request.getName() + "' already exists");
        }
        if (request.isDefaultTemplate() && !template.isDefaultTemplate()) {
            clearDefaultFlag();
        }
        mapper.updateTemplate(template, request);
        resolveAssociations(template, request);
        template = templateRepository.save(template);
        log.info("Updated onboarding template: {}", template.getName());
        return mapper.toTemplateResponse(template);
    }

    @Override
    public void delete(UUID id) {
        OnboardingTemplate template = findTemplateOrThrow(id);
        templateRepository.delete(template);
        log.info("Deleted onboarding template: {}", template.getName());
    }

    @Override
    public OnboardingTaskDefinitionResponse addTaskDefinition(UUID templateId, OnboardingTaskDefinitionRequest request) {
        OnboardingTemplate template = findTemplateOrThrow(templateId);
        OnboardingTaskDefinition definition = mapper.toTaskDefinitionEntity(request);
        definition.setTemplate(template);
        definition = taskDefinitionRepository.save(definition);
        log.info("Added task definition '{}' to template '{}'", definition.getTitle(), template.getName());
        return mapper.toTaskDefinitionResponse(definition);
    }

    @Override
    public OnboardingTaskDefinitionResponse updateTaskDefinition(UUID defId, OnboardingTaskDefinitionRequest request) {
        OnboardingTaskDefinition definition = findDefinitionOrThrow(defId);
        definition.setStepNumber(request.getStepNumber());
        definition.setTitle(request.getTitle());
        definition.setDescription(request.getDescription());
        definition.setTaskType(request.getTaskType());
        definition.setDueDaysFromJoining(request.getDueDaysFromJoining());
        definition.setMandatory(request.isMandatory());
        definition.setResponsibleRole(request.getResponsibleRole());
        if (request.getTemplateId() != null && !request.getTemplateId().equals(definition.getTemplate().getId())) {
            OnboardingTemplate template = findTemplateOrThrow(request.getTemplateId());
            definition.setTemplate(template);
        }
        definition = taskDefinitionRepository.save(definition);
        log.info("Updated task definition: {}", definition.getTitle());
        return mapper.toTaskDefinitionResponse(definition);
    }

    @Override
    public void deleteTaskDefinition(UUID defId) {
        OnboardingTaskDefinition definition = findDefinitionOrThrow(defId);
        taskDefinitionRepository.delete(definition);
        log.info("Deleted task definition: {}", definition.getTitle());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OnboardingTaskDefinitionResponse> getTaskDefinitions(UUID templateId) {
        findTemplateOrThrow(templateId);
        return taskDefinitionRepository.findByTemplateIdOrderByStepNumberAsc(templateId).stream()
                .map(mapper::toTaskDefinitionResponse)
                .toList();
    }

    private void clearDefaultFlag() {
        templateRepository.findByDefaultTemplateTrue().ifPresent(existing -> {
            existing.setDefaultTemplate(false);
            templateRepository.save(existing);
        });
    }

    private void resolveAssociations(OnboardingTemplate template, OnboardingTemplateRequest request) {
        if (request.getDepartmentId() != null) {
            template.setDepartment(departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId().toString())));
        } else {
            template.setDepartment(null);
        }
        if (request.getDesignationId() != null) {
            template.setDesignation(designationRepository.findById(request.getDesignationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Designation", "id", request.getDesignationId().toString())));
        } else {
            template.setDesignation(null);
        }
    }

    private OnboardingTemplate findTemplateOrThrow(UUID id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OnboardingTemplate", "id", id.toString()));
    }

    private OnboardingTaskDefinition findDefinitionOrThrow(UUID id) {
        return taskDefinitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OnboardingTaskDefinition", "id", id.toString()));
    }
}
