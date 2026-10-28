package com.example.userprojectjdbcsqlserver.exception;

import org.springframework.http.HttpStatus;

public class ProfilNotFoundException extends ApiException {
    public ProfilNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "error.profil.not.found", id);
    }
}
