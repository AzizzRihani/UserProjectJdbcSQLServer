package com.example.userprojectjdbcsqlserver.exception;

import org.springframework.http.HttpStatus;

public class CodeBanqueAlreadyExistsException extends ApiException {
    public CodeBanqueAlreadyExistsException(String codeBanque) {
        super(HttpStatus.BAD_REQUEST, "error.code.banque.already.exists", codeBanque);
    }
}
