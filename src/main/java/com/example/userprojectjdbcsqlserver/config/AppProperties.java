package com.example.userprojectjdbcsqlserver.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * PARAMETRES D'INITIALISATION : lus UNE fois au demarrage depuis application.yml (ou variables d'environnement,
 * arguments de ligne de commande, profils), types et valides. Une valeur invalide (ex. app.parameters.cache-ttl: 0s)
 * EMPECHE le demarrage avec un message explicite - contrairement au legacy (CPConfigFile.properties, tout en chaine,
 * aucune validation, erreur decouverte a la premiere requete).
 *
 * Ils decrivent comment l'application demarre et s'expose. Ce qui se regle a chaud par un administrateur est
 * dans la base : voir ParameterService (parametres internes).
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @Valid @NotNull @DefaultValue Session session,
        @Valid @NotNull @DefaultValue Cors cors,
        @Valid @NotNull @DefaultValue Parameters parameters,
        @Valid @NotNull @DefaultValue Sso sso) {

    /** Duree d'inactivite par defaut d'une session et duree de vie du cookie (la duree par session vient de la base). */
    public record Session(
            @NotNull @DurationMin(minutes = 1) @DurationMax(days = 30) @DefaultValue("3d") Duration defaultTimeout) {
    }

    /** Origines autorisees a appeler l'API depuis un navigateur (frontend). */
    public record Cors(@NotEmpty @DefaultValue("https://localhost:4200") List<String> allowedOrigins) {
    }

    /** Duree de vie maximale d'une valeur dans le cache du service de parametres internes. */
    public record Parameters(
            @NotNull @DurationMin(seconds = 1) @DurationMax(hours = 24) @DefaultValue("5m") Duration cacheTtl) {
    }

    /**
     * Connexion SSO OIDC (Google, Microsoft Entra ID). Desactive par defaut : l'application demarre alors sans aucun
     * identifiant. Les secrets viennent uniquement de variables d'environnement (jamais d'un fichier versionne).
     * Un fournisseur dont le clientId est vide est ignore (voir SsoConfig).
     */
    public record Sso(
            @DefaultValue("false") boolean enabled,
            // Frontend vers lequel le navigateur est renvoye apres la connexion SSO (succes ou echec) : valeur de
            // configuration uniquement, jamais fournie par la requete (pas de redirection ouverte).
            @NotEmpty @DefaultValue("https://localhost:4200") String frontendUrl,
            @Valid @NotNull @DefaultValue Google google,
            @Valid @NotNull @DefaultValue Microsoft microsoft) {

        public record Google(String clientId, String clientSecret) {
            public boolean configured() {
                return clientId != null && !clientId.isBlank();
            }
        }

        // Un seul tenant (organisation). authority est surchargeable : cloud souverain, ou faux fournisseur de test.
        public record Microsoft(
                String tenantId,
                String clientId,
                String clientSecret,
                @NotEmpty @DefaultValue("https://login.microsoftonline.com") String authority) {

            public boolean configured() {
                return clientId != null && !clientId.isBlank();
            }
        }
    }
}
