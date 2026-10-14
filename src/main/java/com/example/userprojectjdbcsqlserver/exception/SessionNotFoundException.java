package com.example.userprojectjdbcsqlserver.exception;

import org.springframework.http.HttpStatus;

public class SessionNotFoundException extends ApiException {
    public SessionNotFoundException(String id) {
        super(HttpStatus.NOT_FOUND, "error.session.not.found", id);
    }
}
