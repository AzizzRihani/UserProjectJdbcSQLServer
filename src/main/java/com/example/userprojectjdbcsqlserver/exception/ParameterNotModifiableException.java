package com.example.userprojectjdbcsqlserver.exception;

import org.springframework.http.HttpStatus;

public class ParameterNotModifiableException extends ApiException {
    public ParameterNotModifiableException(String code) {
        super(HttpStatus.FORBIDDEN, "error.parameter.not.modifiable", code);
    }
}
