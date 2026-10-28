package com.example.userprojectjdbcsqlserver.exception;

import org.springframework.http.HttpStatus;

public class FonctionNotFoundException extends ApiException {
    public FonctionNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "error.fonction.not.found", id);
    }
}
