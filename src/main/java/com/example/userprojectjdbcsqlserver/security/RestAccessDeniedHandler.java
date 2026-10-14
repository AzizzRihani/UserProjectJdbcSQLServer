package com.example.userprojectjdbcsqlserver.security;

import tools.jackson.databind.ObjectMapper;
import com.example.userprojectjdbcsqlserver.exception.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

import java.io.IOException;
import java.util.Locale;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;
    private final LocaleResolver localeResolver;

    public RestAccessDeniedHandler(ObjectMapper objectMapper, MessageSource messageSource, LocaleResolver localeResolver) {
        this.objectMapper = objectMapper;
        this.messageSource = messageSource;
        this.localeResolver = localeResolver;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException, ServletException {
        // S'execute avant DispatcherServlet (filtre Spring Security) : LocaleContextHolder n'est pas
        // encore peuple, il faut resoudre la locale directement depuis la requete.
        Locale locale = localeResolver.resolveLocale(request);
        String message = messageSource.getMessage("error.access.denied", null, locale);
        ErrorResponse body = ErrorResponse.of(HttpStatus.FORBIDDEN, message, request.getRequestURI());
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
