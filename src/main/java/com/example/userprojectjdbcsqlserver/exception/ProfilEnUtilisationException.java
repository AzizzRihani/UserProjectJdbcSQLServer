package com.example.userprojectjdbcsqlserver.exception;

import org.springframework.http.HttpStatus;

public class ProfilEnUtilisationException extends ApiException {
    public ProfilEnUtilisationException(Long id) {
        super(HttpStatus.CONFLICT, "error.profil.en.utilisation", id);
    }
}
