package com.hrms.leave.entity;

import com.hrms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "leave_type_tenure_tiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveTypeTenureTier extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "min_years", nullable = false)
    private int minYears;

    /** Exclusive upper bound; null means unbounded (e.g. "5+ years"). */
    @Column(name = "max_years")
    private Integer maxYears;

    @Column(name = "days", nullable = false)
    private int days;
}
