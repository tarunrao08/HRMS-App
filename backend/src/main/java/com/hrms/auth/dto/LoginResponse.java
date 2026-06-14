// Response body returned after successful login or token refresh, carrying both token values and user metadata
package com.hrms.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {

    private String       accessToken;
    private String       refreshToken;
    private String       tokenType;
    private long         expiresIn;
    private String       username;
    private String       email;
    private List<String> roles;
    private java.util.UUID employeeId;
}
