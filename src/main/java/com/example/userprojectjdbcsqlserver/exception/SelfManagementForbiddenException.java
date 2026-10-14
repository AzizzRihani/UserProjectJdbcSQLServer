package com.example.userprojectjdbcsqlserver.exception;

import org.springframework.http.HttpStatus;

public class SelfManagementForbiddenException extends ApiException {
    public SelfManagementForbiddenException() {
        super(HttpStatus.FORBIDDEN, "error.self.management.forbidden");
    }
}
