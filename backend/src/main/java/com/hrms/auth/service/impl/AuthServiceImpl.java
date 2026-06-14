// Implements credential validation, JWT issuance, rotating refresh tokens, and logout (token invalidation)
package com.hrms.auth.service.impl;

import com.hrms.auth.dto.ChangePasswordRequest;
import com.hrms.auth.dto.LoginRequest;
import com.hrms.auth.dto.LoginResponse;
import com.hrms.auth.dto.RefreshTokenRequest;
import com.hrms.auth.entity.User;
import com.hrms.auth.repository.UserRepository;
import com.hrms.auth.security.JwtTokenProvider;
import com.hrms.auth.service.AuthService;
import com.hrms.common.exception.AppException;
import com.hrms.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository        userRepository;
    private final JwtTokenProvider      jwtTokenProvider;
    private final UserDetailsService    userDetailsService;
    private final PasswordEncoder       passwordEncoder;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", userDetails.getUsername()));

        String accessToken  = jwtTokenProvider.generateAccessToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(Instant.now().plusMillis(jwtTokenProvider.getRefreshExpirationMs()));
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        log.info("User '{}' logged in successfully", user.getUsername());
        return buildResponse(accessToken, refreshToken, user, userDetails);
    }

    @Override
    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        User user = userRepository.findByRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new AppException("Invalid refresh token", HttpStatus.UNAUTHORIZED, "INVALID_TOKEN"));

        if (user.getRefreshTokenExpiry() == null || user.getRefreshTokenExpiry().isBefore(Instant.now())) {
            throw new AppException("Refresh token has expired. Please log in again.",
                    HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED");
        }

        UserDetails userDetails   = userDetailsService.loadUserByUsername(user.getUsername());
        String newAccessToken  = jwtTokenProvider.generateAccessToken(userDetails);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        // Rotate the refresh token on every use to prevent replay attacks
        user.setRefreshToken(newRefreshToken);
        user.setRefreshTokenExpiry(Instant.now().plusMillis(jwtTokenProvider.getRefreshExpirationMs()));
        userRepository.save(user);

        log.info("Tokens refreshed for user '{}'", user.getUsername());
        return buildResponse(newAccessToken, newRefreshToken, user, userDetails);
    }

    @Override
    @Transactional
    public void logout(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setRefreshToken(null);
            user.setRefreshTokenExpiry(null);
            userRepository.save(user);
            log.info("User '{}' logged out", username);
        });
    }

    @Override
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AppException("Current password is incorrect", HttpStatus.BAD_REQUEST, "INVALID_PASSWORD");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user '{}'", username);
    }

    private LoginResponse buildResponse(String accessToken, String refreshToken,
                                        User user, UserDetails userDetails) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessExpirationMs())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(userDetails.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .collect(Collectors.toList()))
                .employeeId(user.getEmployeeId())
                .build();
    }
}
