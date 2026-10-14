package com.example.userprojectjdbcsqlserver.exception;

import org.springframework.http.HttpStatus;

public class RoleEnUtilisationException extends ApiException {
    public RoleEnUtilisationException(Long id) {
        super(HttpStatus.CONFLICT, "error.role.en.utilisation", id);
    }
}
