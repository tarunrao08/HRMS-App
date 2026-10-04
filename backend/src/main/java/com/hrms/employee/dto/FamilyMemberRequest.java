package com.hrms.employee.dto;

import com.hrms.common.util.PhoneUtils;
import com.hrms.employee.enums.Gender;
import com.hrms.employee.enums.Relationship;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class FamilyMemberRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 150)
    private String name;

    @NotNull(message = "Relationship is required")
    private Relationship relationship;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private Gender gender;

    @Size(max = 100)
    private String occupation;

    @Pattern(regexp = PhoneUtils.INDIAN_MOBILE_REGEX, message = "Contact number must be a valid 10-digit Indian mobile number")
    private String contactNumber;

    public void setContactNumber(String contactNumber) {
        this.contactNumber = PhoneUtils.normalize(contactNumber);
    }
}
