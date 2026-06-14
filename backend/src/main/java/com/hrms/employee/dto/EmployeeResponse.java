package com.hrms.employee.dto;

import com.hrms.employee.enums.EmploymentStatus;
import com.hrms.employee.enums.EmploymentType;
import com.hrms.employee.enums.Gender;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class EmployeeResponse {
    private UUID id;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private LocalDate joiningDate;
    private LocalDate resignationDate;
    private EmploymentStatus employmentStatus;
    private EmploymentType employmentType;

    private UUID departmentId;
    private String departmentName;
    private UUID designationId;
    private String designationTitle;
    private UUID branchId;
    private String branchName;
    private UUID managerId;
    private String managerName;

    private String profilePictureUrl;
    private String panNumber;
    private String aadharNumber;
    private String bankAccountNumber;
    private String bankIfscCode;
    private String bankName;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelation;

    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
}
