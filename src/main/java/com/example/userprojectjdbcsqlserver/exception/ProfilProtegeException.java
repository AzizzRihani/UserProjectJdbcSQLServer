package com.example.userprojectjdbcsqlserver.exception;

import org.springframework.http.HttpStatus;

public class ProfilProtegeException extends ApiException {
    public ProfilProtegeException() {
        super(HttpStatus.FORBIDDEN, "error.profil.protege");
    }
}
