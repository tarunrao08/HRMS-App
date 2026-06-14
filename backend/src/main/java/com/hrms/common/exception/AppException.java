// Base runtime exception that carries an HTTP status and an app-level error code for uniform error responses
package com.hrms.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AppException extends RuntimeException {

    private final HttpStatus status;
    private final String     errorCode;

    public AppException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status    = status;
        this.errorCode = errorCode;
    }

    public AppException(String message, HttpStatus status, String errorCode, Throwable cause) {
        super(message, cause);
        this.status    = status;
        this.errorCode = errorCode;
    }
}
