package com.example.userprojectjdbcsqlserver.exception;

import org.springframework.http.HttpStatus;

public class BanqueNotFoundException extends ApiException {
    public BanqueNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "error.banque.not.found", id);
    }
}
