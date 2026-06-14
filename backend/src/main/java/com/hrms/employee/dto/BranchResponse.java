package com.hrms.employee.dto;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class BranchResponse {
    private UUID id;
    private String name;
    private String address;
    private String city;
    private String state;
    private String country;
    private String pincode;
    private Instant createdAt;
    private Instant updatedAt;
}
