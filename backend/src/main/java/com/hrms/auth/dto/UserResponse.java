package com.hrms.auth.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
public class UserResponse {

    private UUID         id;
    private String       username;
    private String       email;
    private UUID         employeeId;
    private String       employeeName;
    private String       employeeCode;
    private List<String> roles;
    private boolean      enabled;
    private Instant      lastLoginAt;
    private Instant      createdAt;
}
