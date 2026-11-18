package com.example.userprojectjdbcsqlserver.security;

import com.example.userprojectjdbcsqlserver.config.AppProperties;
import com.example.userprojectjdbcsqlserver.service.SsoUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

/**
 * Echec du SSO : retour au frontend (page de connexion) avec un CODE court, jamais le detail technique (qui est
 * journalise cote serveur). Seuls les codes que le frontend sait traduire sont transmis ; tout le reste (jeton
 * invalide, nonce errone, refus de l'utilisateur chez le fournisseur...) devient SSO_FAILED.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.sso", name = "enabled", havingValue = "true")
public class SsoAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private static final Set<String> FRONTEND_CODES = Set.of(
            SsoUserResolver.CODE_USER_UNKNOWN, SsoUserResolver.CODE_ACCOUNT_LOCKED, SsoUserResolver.CODE_FAILED);

    private final String frontendUrl;

    public SsoAuthenticationFailureHandler(AppProperties appProperties) {
        this.frontendUrl = stripTrailingSlash(appProperties.sso().frontendUrl());
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String code = SsoUserResolver.CODE_FAILED;
        if (exception instanceof OAuth2AuthenticationException oauth
                && FRONTEND_CODES.contains(oauth.getError().getErrorCode())) {
            code = oauth.getError().getErrorCode();
        }
        log.warn("Connexion SSO refusee ({}) : {}", code, exception.getMessage());
        response.sendRedirect(frontendUrl + "/login?sso_error=" + code);
    }

    static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
