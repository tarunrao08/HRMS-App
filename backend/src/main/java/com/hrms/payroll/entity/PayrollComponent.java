package com.hrms.payroll.entity;

import com.hrms.common.entity.BaseEntity;
import com.hrms.payroll.enums.CalculationType;
import com.hrms.payroll.enums.ComponentType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "payroll_components")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollComponent extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "component_type", nullable = false, length = 20)
    private ComponentType componentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_type", nullable = false, length = 20)
    private CalculationType calculationType;

    @Column(name = "value", precision = 12, scale = 4)
    private BigDecimal value;

    @Column(name = "percentage_of", length = 50)
    private String percentageOf;

    @Column(name = "is_taxable")
    private boolean taxable;

    @Column(name = "is_active")
    private boolean active;

    @Column(name = "display_order")
    private int displayOrder;
}
