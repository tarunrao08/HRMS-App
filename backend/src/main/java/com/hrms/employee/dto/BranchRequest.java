package com.hrms.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BranchRequest {

    @NotBlank(message = "Branch name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    private String address;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String state;

    @Size(max = 100)
    private String country;

    @Size(max = 10)
    private String pincode;
}
