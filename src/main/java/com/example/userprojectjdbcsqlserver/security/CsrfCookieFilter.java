package com.example.userprojectjdbcsqlserver.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * CsrfToken est resolu paresseusement par Spring Security (DeferredCsrfToken) : sans acces explicite
 * a getToken(), le cookie XSRF-TOKEN n'est jamais ecrit dans la reponse. Ce filtre force cette resolution
 * a chaque requete pour garantir que le cookie est pose des le premier appel (y compris un /auth/me en 401).
 *
 * Le token est AUSSI recopie dans un header de reponse (X-CSRF-TOKEN) : le cookie XSRF-TOKEN est Secure,
 * donc pas forcement lisible via document.cookie depuis une page servie en http (frontend en dev sur un
 * scheme different du backend). Le frontend lit ce header plutot que le cookie pour rester fiable quel
 * que soit le contexte de securite du navigateur.
 */

@Component
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            response.setHeader("X-CSRF-TOKEN", csrfToken.getToken());
        }
        filterChain.doFilter(request, response);
    }
}
