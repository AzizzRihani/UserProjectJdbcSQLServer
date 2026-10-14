package com.example.userprojectjdbcsqlserver.exception;

import org.springframework.http.HttpStatus;

public class CompteNotFoundException extends ApiException {
    public CompteNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "error.compte.not.found", id);
    }
}
