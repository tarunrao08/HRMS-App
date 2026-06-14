package com.hrms.auth.service;

import com.hrms.auth.dto.CreateUserRequest;
import com.hrms.auth.dto.UserResponse;

import java.util.List;
import java.util.UUID;

public interface UserManagementService {

    UserResponse createUser(CreateUserRequest request);

    List<UserResponse> getAll();

    UserResponse resetPassword(UUID userId, String newPassword);
}
