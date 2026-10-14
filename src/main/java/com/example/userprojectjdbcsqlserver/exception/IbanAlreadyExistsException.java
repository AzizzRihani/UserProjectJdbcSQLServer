package com.example.userprojectjdbcsqlserver.exception;

import org.springframework.http.HttpStatus;

public class IbanAlreadyExistsException extends ApiException {
    public IbanAlreadyExistsException(String iban) {
        super(HttpStatus.BAD_REQUEST, "error.iban.already.exists", iban);
    }
}
