package com.example.userprojectjdbcsqlserver.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        String message = messageSource.getMessage(ex.getMessageKey(), ex.getArgs(), LocaleContextHolder.getLocale());
        ErrorResponse body = ErrorResponse.of(ex.getStatus(), message, request.getRequestURI(), ex.getCode());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler({AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        String message = messageSource.getMessage("error.access.denied", null, LocaleContextHolder.getLocale());
        return build(HttpStatus.FORBIDDEN, message, request);
    }

    @ExceptionHandler({BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        String message = messageSource.getMessage("error.bad.credentials", null, LocaleContextHolder.getLocale());
        return build(HttpStatus.UNAUTHORIZED, message, request);
    }

    // Compte verrouille (security.maxFailedAttempts) : levee AVANT la verification du mot de passe, donc meme avec le bon
    // mot de passe. Code machine pour que le frontend distingue ce cas d'un simple mauvais mot de passe.
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLocked(LockedException ex, HttpServletRequest request) {
        String message = messageSource.getMessage("error.account.locked", null, LocaleContextHolder.getLocale());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED, message, request.getRequestURI(), "ACCOUNT_LOCKED"));
    }

    // Mot de passe expire (security.passwordMaxAgeDays) : levee APRES un mot de passe correct, donc aucune information
    // n'est revelee a quelqu'un qui ne le connait pas.
    @ExceptionHandler(CredentialsExpiredException.class)
    public ResponseEntity<ErrorResponse> handleCredentialsExpired(CredentialsExpiredException ex, HttpServletRequest request) {
        String message = messageSource.getMessage("error.password.expired", null, LocaleContextHolder.getLocale());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED, message, request.getRequestURI(), "PASSWORD_EXPIRED"));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex, HttpServletRequest request) {
        // Seul le prefixe est traduit : ex.getMessage() est du texte brut du framework, non traduisible.
        // L'espace separateur est ajoute ici plutot que de dependre d'un espace final dans le .properties
        // (trim silencieusement au chargement par Properties.load()).
        String prefix = messageSource.getMessage("error.bad.request.prefix", null, LocaleContextHolder.getLocale());
        return build(HttpStatus.BAD_REQUEST, prefix + " " + ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Erreur inattendue sur {}", request.getRequestURI(), ex);
        String message = messageSource.getMessage("error.internal.unexpected", null, LocaleContextHolder.getLocale());
        return build(HttpStatus.INTERNAL_SERVER_ERROR, message, request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(status, message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
