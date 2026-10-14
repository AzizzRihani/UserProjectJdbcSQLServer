package com.example.userprojectjdbcsqlserver.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends ApiException {
    public UserNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "error.user.not.found", id);
    }
}
