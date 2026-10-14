package com.example.userprojectjdbcsqlserver.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends ApiException {
    public EmailAlreadyExistsException(String email) {
        super(HttpStatus.BAD_REQUEST, "error.email.already.exists", email);
    }
}
