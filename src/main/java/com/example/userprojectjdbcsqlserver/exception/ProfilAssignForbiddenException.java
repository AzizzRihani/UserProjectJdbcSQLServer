package com.example.userprojectjdbcsqlserver.exception;

import org.springframework.http.HttpStatus;

public class ProfilAssignForbiddenException extends ApiException {
    public ProfilAssignForbiddenException() {
        super(HttpStatus.FORBIDDEN, "error.profil.assign.forbidden");
    }
}
