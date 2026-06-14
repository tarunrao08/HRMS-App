package com.hrms.attendance.entity;

import com.hrms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "shifts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shift extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "grace_period_minutes", nullable = false)
    @Builder.Default
    private int gracePeriodMinutes = 15;

    @Column(name = "working_hours", nullable = false, precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal workingHours = BigDecimal.valueOf(8.0);

    @Column(name = "is_night_shift", nullable = false)
    @Builder.Default
    private boolean nightShift = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
