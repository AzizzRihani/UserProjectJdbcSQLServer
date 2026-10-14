package com.example.userprojectjdbcsqlserver.exception;

import org.springframework.http.HttpStatus;

public class BanqueEnUtilisationException extends ApiException {
    public BanqueEnUtilisationException(Long id) {
        super(HttpStatus.CONFLICT, "error.banque.en.utilisation", id);
    }
}
