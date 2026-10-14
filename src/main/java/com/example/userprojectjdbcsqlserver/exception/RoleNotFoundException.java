package com.example.userprojectjdbcsqlserver.exception;

import org.springframework.http.HttpStatus;

public class RoleNotFoundException extends ApiException {
    public RoleNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "error.role.not.found", id);
    }
}
