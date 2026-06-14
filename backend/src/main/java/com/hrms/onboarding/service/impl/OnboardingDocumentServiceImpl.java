package com.hrms.onboarding.service.impl;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.exception.ValidationException;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.onboarding.dto.OnboardingDocumentRequest;
import com.hrms.onboarding.dto.OnboardingDocumentResponse;
import com.hrms.onboarding.entity.OnboardingDocument;
import com.hrms.onboarding.entity.OnboardingTask;
import com.hrms.onboarding.entity.OnboardingWorkflow;
import com.hrms.onboarding.mapper.OnboardingMapper;
import com.hrms.onboarding.repository.OnboardingDocumentRepository;
import com.hrms.onboarding.repository.OnboardingTaskRepository;
import com.hrms.onboarding.repository.OnboardingWorkflowRepository;
import com.hrms.onboarding.service.OnboardingDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OnboardingDocumentServiceImpl implements OnboardingDocumentService {

    private final OnboardingDocumentRepository documentRepository;
    private final OnboardingTaskRepository     taskRepository;
    private final OnboardingWorkflowRepository workflowRepository;
    private final EmployeeRepository           employeeRepository;
    private final OnboardingMapper             mapper;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public OnboardingDocumentResponse upload(OnboardingDocumentRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getEmployeeId().toString()));

        OnboardingTask task = null;
        if (request.getTaskId() != null) {
            task = taskRepository.findById(request.getTaskId())
                    .orElseThrow(() -> new ResourceNotFoundException("OnboardingTask", "id", request.getTaskId().toString()));
        }

        OnboardingDocument document = OnboardingDocument.builder()
                .employee(employee)
                .task(task)
                .documentType(request.getDocumentType())
                .documentName(request.getDocumentName())
                .fileUrl(request.getFileUrl())
                .fileSizeBytes(request.getFileSizeBytes())
                .mimeType(request.getMimeType())
                .uploadedAt(Instant.now())
                .verified(false)
                .build();

        document = documentRepository.save(document);
        log.info("Uploaded document '{}' for employee: {}", document.getDocumentName(), employee.getEmployeeCode());
        return mapper.toDocumentResponse(document);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OnboardingDocumentResponse> getByEmployee(UUID employeeId) {
        return documentRepository.findByEmployeeIdOrderByUploadedAtDesc(employeeId).stream()
                .map(mapper::toDocumentResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OnboardingDocumentResponse> getByTask(UUID taskId) {
        return documentRepository.findByTaskIdOrderByUploadedAtDesc(taskId).stream()
                .map(mapper::toDocumentResponse)
                .toList();
    }

    @Override
    public OnboardingDocumentResponse verify(UUID documentId, UUID verifiedByUserId) {
        OnboardingDocument document = findDocumentOrThrow(documentId);
        document.setVerified(true);
        document.setVerifiedBy(verifiedByUserId);
        document.setVerifiedAt(Instant.now());
        document.setRejectionReason(null);
        document = documentRepository.save(document);
        log.info("Verified document: {}", documentId);
        return mapper.toDocumentResponse(document);
    }

    @Override
    public OnboardingDocumentResponse reject(UUID documentId, String rejectionReason) {
        OnboardingDocument document = findDocumentOrThrow(documentId);
        document.setVerified(false);
        document.setRejectionReason(rejectionReason);
        document = documentRepository.save(document);
        log.info("Rejected document: {} — reason: {}", documentId, rejectionReason);
        return mapper.toDocumentResponse(document);
    }

    @Override
    public OnboardingDocumentResponse uploadFile(UUID workflowId, UUID taskId, String documentType,
                                                 MultipartFile file, UUID uploadedByUserId) {
        OnboardingWorkflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("OnboardingWorkflow", "id", workflowId.toString()));

        OnboardingTask task = null;
        if (taskId != null) {
            task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new ResourceNotFoundException("OnboardingTask", "id", taskId.toString()));
            if (!task.getWorkflow().getId().equals(workflowId)) {
                throw new ValidationException("Task " + taskId + " does not belong to workflow " + workflowId);
            }
        }

        String originalFilename = file.getOriginalFilename() != null
                ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                : "file";
        String storedFilename = UUID.randomUUID() + "_" + originalFilename;

        try {
            Path dir = Paths.get(uploadDir, "onboarding", workflowId.toString());
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), dir.resolve(storedFilename), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new ValidationException("Failed to store uploaded file: " + ex.getMessage());
        }

        String fileUrl = uploadDir + "/onboarding/" + workflowId + "/" + storedFilename;

        OnboardingDocument document = OnboardingDocument.builder()
                .employee(workflow.getEmployee())
                .task(task)
                .documentType(documentType)
                .documentName(originalFilename)
                .fileUrl(fileUrl)
                .fileSizeBytes(file.getSize())
                .mimeType(file.getContentType())
                .uploadedAt(Instant.now())
                .verified(false)
                .build();

        document = documentRepository.save(document);
        log.info("Stored file '{}' for workflow: {}", storedFilename, workflowId);
        return mapper.toDocumentResponse(document);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OnboardingDocumentResponse> getByWorkflow(UUID workflowId) {
        OnboardingWorkflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("OnboardingWorkflow", "id", workflowId.toString()));
        return documentRepository.findByEmployeeIdOrderByUploadedAtDesc(workflow.getEmployee().getId())
                .stream().map(mapper::toDocumentResponse).toList();
    }

    private OnboardingDocument findDocumentOrThrow(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OnboardingDocument", "id", id.toString()));
    }
}
