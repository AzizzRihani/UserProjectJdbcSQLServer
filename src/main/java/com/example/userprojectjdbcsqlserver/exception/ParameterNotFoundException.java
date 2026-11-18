package com.example.userprojectjdbcsqlserver.exception;

import org.springframework.http.HttpStatus;

public class ParameterNotFoundException extends ApiException {
    public ParameterNotFoundException(String code) {
        super(HttpStatus.NOT_FOUND, "error.parameter.not.found", code);
    }
}
