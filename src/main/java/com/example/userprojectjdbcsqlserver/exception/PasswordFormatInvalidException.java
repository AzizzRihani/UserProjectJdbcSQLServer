package com.example.userprojectjdbcsqlserver.exception;

import org.springframework.http.HttpStatus;

public class PasswordFormatInvalidException extends ApiException {
    public PasswordFormatInvalidException() {
        super(HttpStatus.BAD_REQUEST, "error.password.format.invalid");
    }
}
