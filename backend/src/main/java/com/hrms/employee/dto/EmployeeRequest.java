package com.hrms.employee.dto;

import com.hrms.employee.enums.EmploymentStatus;
import com.hrms.employee.enums.EmploymentType;
import com.hrms.employee.enums.Gender;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class EmployeeRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 150)
    private String email;

    @Size(max = 15)
    private String phone;

    private LocalDate dateOfBirth;

    private Gender gender;

    private String address;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String state;

    @Size(max = 10)
    private String pincode;

    @NotNull(message = "Joining date is required")
    private LocalDate joiningDate;

    private LocalDate resignationDate;

    private EmploymentStatus employmentStatus;

    private EmploymentType employmentType;

    private UUID departmentId;

    private UUID designationId;

    private UUID branchId;

    private UUID managerId;

    private String profilePictureUrl;

    @Pattern(regexp = "[A-Z]{5}[0-9]{4}[A-Z]{1}", message = "PAN must be in format: ABCDE1234F")
    private String panNumber;

    @Pattern(regexp = "\\d{12}", message = "Aadhar number must be exactly 12 digits")
    private String aadharNumber;

    @Size(max = 20)
    private String bankAccountNumber;

    @Pattern(regexp = "[A-Z]{4}0[A-Z0-9]{6}", message = "IFSC code must be in format: ABCD0123456")
    private String bankIfscCode;

    @Size(max = 100)
    private String bankName;

    @Size(max = 100)
    private String emergencyContactName;

    @Size(max = 15)
    private String emergencyContactPhone;

    @Size(max = 50)
    private String emergencyContactRelation;
}
