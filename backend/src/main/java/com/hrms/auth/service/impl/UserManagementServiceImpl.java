package com.hrms.auth.service.impl;

import com.hrms.auth.dto.CreateUserRequest;
import com.hrms.auth.dto.UserResponse;
import com.hrms.auth.entity.Role;
import com.hrms.auth.entity.User;
import com.hrms.auth.repository.RoleRepository;
import com.hrms.auth.repository.UserRepository;
import com.hrms.auth.service.UserManagementService;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.exception.ValidationException;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository     userRepository;
    private final RoleRepository     roleRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder    passwordEncoder;

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getEmployeeId().toString()));

        if (userRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new ValidationException("Employee " + employee.getEmployeeCode() + " already has a login account");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ValidationException("Username '" + request.getUsername() + "' is already taken");
        }

        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", request.getRole()));

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(employee.getEmail())
                .employeeId(employee.getId())
                .build();
        user.getRoles().add(role);

        user = userRepository.save(user);
        log.info("Created login account '{}' for employee: {}", user.getUsername(), employee.getEmployeeCode());
        return toResponse(user, employee);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userRepository.findAll().stream()
                .map(user -> {
                    Employee emp = user.getEmployeeId() != null
                            ? employeeRepository.findById(user.getEmployeeId()).orElse(null)
                            : null;
                    return toResponse(user, emp);
                })
                .toList();
    }

    @Override
    public UserResponse resetPassword(UUID userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));
        user.setPassword(passwordEncoder.encode(newPassword));
        user = userRepository.save(user);
        Employee emp = user.getEmployeeId() != null
                ? employeeRepository.findById(user.getEmployeeId()).orElse(null)
                : null;
        log.info("Password reset for user: {}", user.getUsername());
        return toResponse(user, emp);
    }

    private UserResponse toResponse(User user, Employee employee) {
        UserResponse r = new UserResponse();
        r.setId(user.getId());
        r.setUsername(user.getUsername());
        r.setEmail(user.getEmail());
        r.setEmployeeId(user.getEmployeeId());
        r.setEmployeeName(employee != null
                ? employee.getFirstName() + " " + employee.getLastName() : null);
        r.setEmployeeCode(employee != null ? employee.getEmployeeCode() : null);
        r.setRoles(user.getRoles().stream().map(Role::getName).toList());
        r.setEnabled(user.isEnabled());
        r.setLastLoginAt(user.getLastLoginAt());
        r.setCreatedAt(user.getCreatedAt());
        return r;
    }
}
