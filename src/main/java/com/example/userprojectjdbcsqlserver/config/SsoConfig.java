package com.example.userprojectjdbcsqlserver.config;

import com.example.userprojectjdbcsqlserver.service.SsoUserResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

import java.util.ArrayList;
import java.util.List;

/**
 * Inscriptions OIDC (Google, Microsoft Entra ID), construites a partir des parametres d'initialisation app.sso.*
 * plutot que de spring.security.oauth2.client.* : chaque fournisseur est facultatif (clientId vide = ignore), et
 * l'application demarre sans aucune variable quand le SSO est desactive.
 * Aucun appel reseau au demarrage : les points d'acces de Microsoft sont calcules, pas decouverts.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.sso", name = "enabled", havingValue = "true")
public class SsoConfig {

    @Bean
    public InMemoryClientRegistrationRepository clientRegistrationRepository(AppProperties appProperties) {
        AppProperties.Sso sso = appProperties.sso();
        List<ClientRegistration> registrations = new ArrayList<>();
        if (sso.google().configured()) {
            registrations.add(google(sso.google()));
        }
        if (sso.microsoft().configured()) {
            registrations.add(microsoft(sso.microsoft()));
        }
        if (registrations.isEmpty()) {
            throw new IllegalStateException("app.sso.enabled=true mais aucun fournisseur n'est configure : renseignez "
                    + "GOOGLE_CLIENT_ID/GOOGLE_CLIENT_SECRET et/ou MICROSOFT_TENANT_ID/MICROSOFT_CLIENT_ID/"
                    + "MICROSOFT_CLIENT_SECRET, ou desactivez le SSO (SSO_ENABLED=false)");
        }
        return new InMemoryClientRegistrationRepository(registrations);
    }

    private static ClientRegistration google(AppProperties.Sso.Google google) {
        require(google.clientSecret(), "GOOGLE_CLIENT_SECRET (app.sso.google.client-secret) est obligatoire quand GOOGLE_CLIENT_ID est renseigne");
        return CommonOAuth2Provider.GOOGLE.getBuilder(SsoUserResolver.GOOGLE)
                .clientId(google.clientId())
                .clientSecret(google.clientSecret())
                .build();
    }

    private static ClientRegistration microsoft(AppProperties.Sso.Microsoft microsoft) {
        require(microsoft.clientSecret(), "MICROSOFT_CLIENT_SECRET (app.sso.microsoft.client-secret) est obligatoire quand MICROSOFT_CLIENT_ID est renseigne");
        require(microsoft.tenantId(), "MICROSOFT_TENANT_ID (app.sso.microsoft.tenant-id) est obligatoire quand MICROSOFT_CLIENT_ID est renseigne");
        // Le claim "iss" des jetons Entra contient l'identifiant du tenant en minuscules : on normalise pour que la
        // verification de l'issuer par Spring ne depende pas de la casse saisie.
        String base = stripTrailingSlash(microsoft.authority()) + "/" + microsoft.tenantId().trim().toLowerCase();
        return ClientRegistration.withRegistrationId(SsoUserResolver.MICROSOFT)
                .clientId(microsoft.clientId())
                .clientSecret(microsoft.clientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri(base + "/oauth2/v2.0/authorize")
                .tokenUri(base + "/oauth2/v2.0/token")
                .jwkSetUri(base + "/discovery/v2.0/keys")
                .issuerUri(base + "/v2.0")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .clientName("Microsoft")
                .build();
    }

    private static void require(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
