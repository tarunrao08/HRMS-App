package com.hrms.employee.dto;

import com.hrms.common.util.PhoneUtils;
import com.hrms.employee.enums.EmploymentStatus;
import com.hrms.employee.enums.EmploymentType;
import com.hrms.employee.enums.Gender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
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

    @Pattern(regexp = PhoneUtils.INDIAN_MOBILE_REGEX, message = "Phone number must be a valid 10-digit Indian mobile number")
    private String phone;

    public void setPhone(String phone) {
        this.phone = PhoneUtils.normalize(phone);
    }

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private Gender gender;

    private String address;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String state;

    @Pattern(regexp = "^[1-9]\\d{5}$", message = "Pincode must be a valid 6-digit Indian PIN code")
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

    private UUID shiftId;

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

    @Pattern(regexp = PhoneUtils.INDIAN_MOBILE_REGEX, message = "Emergency contact phone must be a valid 10-digit Indian mobile number")
    private String emergencyContactPhone;

    public void setEmergencyContactPhone(String emergencyContactPhone) {
        this.emergencyContactPhone = PhoneUtils.normalize(emergencyContactPhone);
    }

    @Size(max = 50)
    private String emergencyContactRelation;

    @Valid
    private List<FamilyMemberRequest> familyMembers;

    @Valid
    private List<NomineeRequest> nominees;
}
