package com.hrms.employee.dto;

import com.hrms.common.util.PhoneUtils;
import com.hrms.employee.enums.Relationship;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class NomineeRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 150)
    private String name;

    @NotNull(message = "Relationship is required")
    private Relationship relationship;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotNull(message = "Share percentage is required")
    @DecimalMin(value = "0.00", message = "Share percentage must be between 0 and 100")
    @DecimalMax(value = "100.00", message = "Share percentage must be between 0 and 100")
    private BigDecimal sharePercentage;

    private String address;

    @Pattern(regexp = PhoneUtils.INDIAN_MOBILE_REGEX, message = "Contact number must be a valid 10-digit Indian mobile number")
    private String contactNumber;

    public void setContactNumber(String contactNumber) {
        this.contactNumber = PhoneUtils.normalize(contactNumber);
    }

    // Derived server-side from dateOfBirth (see EmployeeNomineeServiceImpl#normalizeAndValidateMinor) —
    // any client-supplied value is overwritten so the flag can't drift from the actual age.
    private boolean minor;

    @Size(max = 150)
    private String guardianName;

    private Relationship guardianRelationship;
}
