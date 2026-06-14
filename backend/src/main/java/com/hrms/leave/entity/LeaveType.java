package com.hrms.leave.entity;

import com.hrms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "leave_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveType extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "max_days_per_year")
    private int maxDaysPerYear;

    @Column(name = "carry_forward_allowed")
    private boolean carryForwardAllowed;

    @Column(name = "max_carry_forward_days")
    private int maxCarryForwardDays;

    @Column(name = "encashment_allowed")
    private boolean encashmentAllowed;

    @Column(name = "is_paid")
    private boolean paid;

    @Column(name = "requires_document")
    private boolean requiresDocument;

    @Column(name = "min_notice_days")
    private int minNoticeDays;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;
}
