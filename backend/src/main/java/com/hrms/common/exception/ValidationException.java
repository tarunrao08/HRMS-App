// Thrown for domain/business-rule validation failures distinct from bean validation; maps to HTTP 422
package com.hrms.common.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends AppException {

    public ValidationException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR");
    }
}
