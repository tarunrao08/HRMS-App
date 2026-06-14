package com.hrms.onboarding.entity;

import com.hrms.common.entity.BaseEntity;
import com.hrms.employee.entity.Department;
import com.hrms.employee.entity.Designation;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "onboarding_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingTemplate extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designation_id")
    private Designation designation;

    @Column(name = "is_default")
    private boolean defaultTemplate;

    @Column(name = "is_active")
    private boolean active;
}
