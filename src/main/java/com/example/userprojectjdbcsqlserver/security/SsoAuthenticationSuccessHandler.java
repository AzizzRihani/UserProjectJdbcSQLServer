package com.example.userprojectjdbcsqlserver.security;

import com.example.userprojectjdbcsqlserver.config.AppProperties;
import com.example.userprojectjdbcsqlserver.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Apres un SSO reussi (Spring a deja enregistre la session dans le SessionRegistry et sauvegarde le contexte) :
 * meme fin de connexion que le login par mot de passe, puis retour au frontend, qui retrouve la session via /auth/me.
 */
@Component
@ConditionalOnProperty(prefix = "app.sso", name = "enabled", havingValue = "true")
public class SsoAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final String frontendUrl;

    public SsoAuthenticationSuccessHandler(AuthService authService, AppProperties appProperties) {
        this.authService = authService;
        this.frontendUrl = SsoAuthenticationFailureHandler.stripTrailingSlash(appProperties.sso().frontendUrl());
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        SecurityUser principal = (SecurityUser) authentication.getPrincipal();
        String provider = authentication instanceof OAuth2AuthenticationToken token
                ? token.getAuthorizedClientRegistrationId() : "sso";
        authService.openSession(principal.getUser(), request.getHeader("User-Agent"), request.getRemoteAddr(),
                request, "SSO " + provider);
        response.sendRedirect(frontendUrl + "/home");
    }
}
