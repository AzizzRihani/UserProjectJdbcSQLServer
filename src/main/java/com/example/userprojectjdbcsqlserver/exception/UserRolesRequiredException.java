package com.example.userprojectjdbcsqlserver.exception;

import org.springframework.http.HttpStatus;

public class UserRolesRequiredException extends ApiException {
    public UserRolesRequiredException() {
        super(HttpStatus.BAD_REQUEST, "error.user.roles.required");
    }
}
