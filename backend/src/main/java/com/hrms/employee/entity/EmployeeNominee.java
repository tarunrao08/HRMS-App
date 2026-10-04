package com.hrms.employee.entity;

import com.hrms.common.entity.BaseEntity;
import com.hrms.employee.enums.Relationship;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employee_nominees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeNominee extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship", nullable = false, length = 20)
    private Relationship relationship;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "share_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal sharePercentage;

    @Column(name = "address")
    private String address;

    @Column(name = "contact_number", length = 15)
    private String contactNumber;

    @Column(name = "is_minor", nullable = false)
    @Builder.Default
    private boolean minor = false;

    @Column(name = "guardian_name", length = 150)
    private String guardianName;

    @Enumerated(EnumType.STRING)
    @Column(name = "guardian_relationship", length = 20)
    private Relationship guardianRelationship;
}
