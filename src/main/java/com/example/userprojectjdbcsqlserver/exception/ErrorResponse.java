package com.example.userprojectjdbcsqlserver.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;

// "code" : identifiant lisible par une machine (ex. FUNCTION_ACCESS_DENIED), null pour la plupart des erreurs.
// Le frontend s'en sert pour distinguer deux 403 qui ont le meme statut (fonction du profil vs permission).
public record ErrorResponse(Instant timestamp, int status, String error, String message, String path, String code) {

    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return of(status, message, path, null);
    }

    public static ErrorResponse of(HttpStatus status, String message, String path, String code) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, path, code);
    }
}
