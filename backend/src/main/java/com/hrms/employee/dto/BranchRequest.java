package com.hrms.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

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

    // The full set of departments this branch offers — treated as a replace-all on every
    // create/update, same convention as familyMembers/nominees on EmployeeRequest.
    private List<UUID> departmentIds;
}
