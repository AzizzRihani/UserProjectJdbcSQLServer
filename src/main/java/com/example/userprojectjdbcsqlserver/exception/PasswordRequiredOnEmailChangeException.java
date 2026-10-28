package com.example.userprojectjdbcsqlserver.exception;

import org.springframework.http.HttpStatus;

public class PasswordRequiredOnEmailChangeException extends ApiException {
    public PasswordRequiredOnEmailChangeException() {
        super(HttpStatus.BAD_REQUEST, "error.user.password.required.on.email.change");
    }
}
