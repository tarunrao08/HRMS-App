// Thrown when the authenticated user lacks permission for an action; maps to HTTP 403
package com.hrms.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends AppException {

    public UnauthorizedException(String message) {
        super(message, HttpStatus.FORBIDDEN, "UNAUTHORIZED_ACCESS");
    }
}
