package com.hrms.employee.dto;

import com.hrms.employee.enums.Relationship;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class NomineeResponse {
    private UUID id;
    private UUID employeeId;
    private String name;
    private Relationship relationship;
    private LocalDate dateOfBirth;
    private BigDecimal sharePercentage;
    private String address;
    private String contactNumber;
    private boolean minor;
    private String guardianName;
    private Relationship guardianRelationship;
}
