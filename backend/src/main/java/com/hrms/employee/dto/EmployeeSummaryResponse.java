package com.hrms.employee.dto;

import com.hrms.employee.enums.EmploymentStatus;
import com.hrms.employee.enums.EmploymentType;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class EmployeeSummaryResponse {
    private UUID id;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDate joiningDate;
    private EmploymentStatus employmentStatus;
    private EmploymentType employmentType;
    private String departmentName;
    private String designationTitle;
    private String branchName;
    private String profilePictureUrl;
}
