package com.example.userprojectjdbcsqlserver.exception;

import org.springframework.http.HttpStatus;

import java.util.Arrays;

public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String messageKey;
    private final Object[] args;

    // Le message stocke dans RuntimeException n'est qu'un diagnostic serveur (logs, debugger) -
    // jamais renvoye au client tel quel : GlobalExceptionHandler resout messageKey+args en texte
    // localise via MessageSource au moment de construire la reponse.
    protected ApiException(HttpStatus status, String messageKey, Object... args) {
        super(messageKey + (args.length > 0 ? " " + Arrays.toString(args) : ""));
        this.status = status;
        this.messageKey = messageKey;
        this.args = args;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessageKey() {
        return messageKey;
    }

    // Code machine optionnel (surcharge dans les sous-classes dont le frontend doit reconnaitre le cas).
    public String getCode() {
        return null;
    }

    public Object[] getArgs() {
        return args;
    }
}
