package com.hrms.onboarding.entity;

import com.hrms.common.entity.BaseEntity;
import com.hrms.employee.entity.Employee;
import com.hrms.onboarding.enums.WorkflowStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "onboarding_workflows", uniqueConstraints = @UniqueConstraint(columnNames = "employee_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingWorkflow extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private OnboardingTemplate template;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private WorkflowStatus status;

    @Column(name = "current_step")
    private int currentStep;

    @Column(name = "total_steps")
    private int totalSteps;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_hr_id")
    private Employee assignedHr;
}
