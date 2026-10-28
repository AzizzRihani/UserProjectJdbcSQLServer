package com.example.userprojectjdbcsqlserver.exception;

import org.springframework.http.HttpStatus;

public class ProfilCodeAlreadyExistsException extends ApiException {
    public ProfilCodeAlreadyExistsException(String code) {
        super(HttpStatus.BAD_REQUEST, "error.profil.code.already.exists", code);
    }
}
