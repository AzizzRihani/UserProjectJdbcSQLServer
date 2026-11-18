package com.example.userprojectjdbcsqlserver.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.stereotype.Component;

/**
 * Branche le flux "Authorization Code + PKCE" de Spring Security (oauth2Login) sur la chaine de securite existante,
 * seulement quand le SSO est active. Le backend est le client confidentiel : le secret ne va jamais au navigateur.
 * Points d'entree : GET /oauth2/authorization/{google|microsoft} (depart) et /login/oauth2/code/{id} (retour).
 */
@Component
@ConditionalOnProperty(prefix = "app.sso", name = "enabled", havingValue = "true")
public class SsoLoginConfigurer {

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final SsoOidcUserService oidcUserService;
    private final SsoAuthenticationSuccessHandler successHandler;
    private final SsoAuthenticationFailureHandler failureHandler;

    public SsoLoginConfigurer(ClientRegistrationRepository clientRegistrationRepository,
                              SsoOidcUserService oidcUserService,
                              SsoAuthenticationSuccessHandler successHandler,
                              SsoAuthenticationFailureHandler failureHandler) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.oidcUserService = oidcUserService;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
    }

    public void configure(HttpSecurity http) throws Exception {
        DefaultOAuth2AuthorizationRequestResolver requestResolver = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, "/oauth2/authorization");
        // PKCE aussi pour un client confidentiel (Spring ne l'active par defaut que pour les clients publics).
        requestResolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());

        http.oauth2Login(oauth -> oauth
                .authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(requestResolver))
                .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService))
                .successHandler(successHandler)
                .failureHandler(failureHandler));
    }
}
