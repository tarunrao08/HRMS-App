// Thrown when a DB lookup by ID or unique field returns empty; maps to HTTP 404
package com.hrms.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends AppException {

    public ResourceNotFoundException(String resource, String field, Object value) {
        super(
            String.format("%s not found with %s: '%s'", resource, field, value),
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND"
        );
    }
}
