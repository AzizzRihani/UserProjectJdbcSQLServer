package com.example.userprojectjdbcsqlserver.exception;

import org.springframework.http.HttpStatus;

public class LoginTimestampInvalidException extends ApiException {
    public LoginTimestampInvalidException() {
        super(HttpStatus.BAD_REQUEST, "error.login.timestamp.invalid");
    }
}
