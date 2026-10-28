package com.example.userprojectjdbcsqlserver.exception;

import org.springframework.http.HttpStatus;

// 403 : l'ecran demande n'appartient pas aux fonctions du profil de l'utilisateur. Distinct d'un refus de
// permission (AccessDeniedException) grace au code FUNCTION_ACCESS_DENIED, que le frontend reconnait.
public class FunctionAccessDeniedException extends ApiException {

    public static final String CODE = "FUNCTION_ACCESS_DENIED";

    public FunctionAccessDeniedException() {
        super(HttpStatus.FORBIDDEN, "error.function.access.denied");
    }

    @Override
    public String getCode() {
        return CODE;
    }
}
