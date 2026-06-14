package com.hrms.onboarding.entity;

import com.hrms.common.entity.BaseEntity;
import com.hrms.onboarding.enums.TaskType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "onboarding_task_definitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingTaskDefinition extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private OnboardingTemplate template;

    @Column(name = "step_number", nullable = false)
    private int stepNumber;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 30)
    private TaskType taskType;

    @Column(name = "due_days_from_joining")
    private int dueDaysFromJoining;

    @Column(name = "is_mandatory")
    private boolean mandatory;

    @Column(name = "responsible_role", length = 50)
    private String responsibleRole;
}
