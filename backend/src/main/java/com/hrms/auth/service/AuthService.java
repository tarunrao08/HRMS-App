// Auth service interface; implementations are swappable without touching controllers
package com.hrms.auth.service;

import com.hrms.auth.dto.ChangePasswordRequest;
import com.hrms.auth.dto.LoginRequest;
import com.hrms.auth.dto.LoginResponse;
import com.hrms.auth.dto.RefreshTokenRequest;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    LoginResponse refreshToken(RefreshTokenRequest request);

    void logout(String username);

    void changePassword(String username, ChangePasswordRequest request);
}
