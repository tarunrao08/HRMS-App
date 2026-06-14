package com.hrms.onboarding.repository;

import com.hrms.onboarding.entity.OnboardingDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OnboardingDocumentRepository extends JpaRepository<OnboardingDocument, UUID> {

    List<OnboardingDocument> findByEmployeeIdOrderByUploadedAtDesc(UUID employeeId);

    List<OnboardingDocument> findByTaskIdOrderByUploadedAtDesc(UUID taskId);
}
