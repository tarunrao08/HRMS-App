package com.hrms.payroll.entity;

import com.hrms.common.entity.BaseEntity;
import com.hrms.payroll.enums.PayrollRunStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payroll_runs", uniqueConstraints = @UniqueConstraint(columnNames = {"year", "month"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollRun extends BaseEntity {

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "month", nullable = false)
    private int month;

    @Column(name = "run_date")
    private Instant runDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private PayrollRunStatus status;

    @Column(name = "total_employees")
    private int totalEmployees;

    @Column(name = "total_gross", precision = 16, scale = 2)
    private BigDecimal totalGross;

    @Column(name = "total_deductions", precision = 16, scale = 2)
    private BigDecimal totalDeductions;

    @Column(name = "total_net", precision = 16, scale = 2)
    private BigDecimal totalNet;

    @Column(name = "processed_by")
    private UUID processedBy;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "disbursed_by")
    private UUID disbursedBy;

    @Column(name = "disbursed_at")
    private Instant disbursedAt;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}
