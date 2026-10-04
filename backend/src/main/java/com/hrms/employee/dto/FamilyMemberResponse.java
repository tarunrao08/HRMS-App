package com.hrms.employee.dto;

import com.hrms.employee.enums.Gender;
import com.hrms.employee.enums.Relationship;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class FamilyMemberResponse {
    private UUID id;
    private UUID employeeId;
    private String name;
    private Relationship relationship;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String occupation;
    private String contactNumber;
}
