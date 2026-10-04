package com.hrms.employee.entity;

import com.hrms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "branches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "country", nullable = false, length = 100)
    @Builder.Default
    private String country = "India";

    @Column(name = "pincode", length = 10)
    private String pincode;

    // The set of departments this branch offers. A department can belong to several branches
    // (and a branch can offer several departments), so this is a plain many-to-many join table
    // with no extra attributes on the association itself.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "branch_departments",
        joinColumns = @JoinColumn(name = "branch_id"),
        inverseJoinColumns = @JoinColumn(name = "department_id")
    )
    @Builder.Default
    private Set<Department> departments = new HashSet<>();
}
