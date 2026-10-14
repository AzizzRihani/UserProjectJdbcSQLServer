package com.example.userprojectjdbcsqlserver.security;

import tools.jackson.databind.ObjectMapper;
import com.example.userprojectjdbcsqlserver.exception.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.session.SessionInformationExpiredEvent;
import org.springframework.security.web.session.SessionInformationExpiredStrategy;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

import java.io.IOException;
import java.util.Locale;

/**
 * Strategie par defaut de ConcurrentSessionFilter : ecrit un texte brut "This session has been
 * expired..." avec un statut 200 (!) quand la session courante vient d'etre revoquee/expiree -
 * trompeur pour un client REST (un 200 laisse croire que la requete a reussi). On la remplace ici
 * par le meme format JSON 401 que RestAuthenticationEntryPoint, pour un comportement coherent
 * qu'une session soit absente (jamais authentifiee) ou revoquee en cours de route (ex. via
 * DELETE /auth/sessions/{id} ou POST /users/{id}/revoke-access).
 */
@Component
public class RestSessionInformationExpiredStrategy implements SessionInformationExpiredStrategy {

    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;
    private final LocaleResolver localeResolver;

    public RestSessionInformationExpiredStrategy(ObjectMapper objectMapper, MessageSource messageSource, LocaleResolver localeResolver) {
        this.objectMapper = objectMapper;
        this.messageSource = messageSource;
        this.localeResolver = localeResolver;
    }

    @Override
    public void onExpiredSessionDetected(SessionInformationExpiredEvent event) throws IOException, ServletException {
        HttpServletResponse response = event.getResponse();
        // S'execute avant DispatcherServlet (filtre Spring Security) : LocaleContextHolder n'est pas
        // encore peuple, il faut resoudre la locale directement depuis la requete.
        Locale locale = localeResolver.resolveLocale(event.getRequest());
        String message = messageSource.getMessage("error.auth.required", null, locale);
        ErrorResponse body = ErrorResponse.of(HttpStatus.UNAUTHORIZED, message, event.getRequest().getRequestURI());
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
