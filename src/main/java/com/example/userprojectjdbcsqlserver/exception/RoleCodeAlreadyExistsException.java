package com.example.userprojectjdbcsqlserver.exception;

import org.springframework.http.HttpStatus;

public class RoleCodeAlreadyExistsException extends ApiException {
    public RoleCodeAlreadyExistsException(String code) {
        super(HttpStatus.BAD_REQUEST, "error.role.code.already.exists", code);
    }
}
