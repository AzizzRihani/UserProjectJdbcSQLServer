package com.example.userprojectjdbcsqlserver.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parametres d'initialisation : une valeur invalide doit EMPECHER le demarrage (le legacy ne detecte l'erreur qu'a la
 * premiere requete). Aucune base ni serveur : seul le binding + la validation de AppProperties est exerce.
 */
class AppPropertiesTest {

    @Configuration
    @EnableConfigurationProperties(AppProperties.class)
    static class Config {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Config.class);

    @Test
    void defaultsApplyWhenNothingIsConfigured() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            AppProperties properties = context.getBean(AppProperties.class);
            assertThat(properties.session().defaultTimeout()).isEqualTo(Duration.ofDays(3));
            assertThat(properties.parameters().cacheTtl()).isEqualTo(Duration.ofMinutes(5));
            assertThat(properties.cors().allowedOrigins()).containsExactly("https://localhost:4200");
        });
    }

    @Test
    void valuesAreBoundWithTheirTypes() {
        runner.withPropertyValues("app.session.default-timeout=2h", "app.parameters.cache-ttl=30s",
                "app.cors.allowed-origins=https://a.example,https://b.example").run(context -> {
            AppProperties properties = context.getBean(AppProperties.class);
            assertThat(properties.session().defaultTimeout()).isEqualTo(Duration.ofHours(2));
            assertThat(properties.parameters().cacheTtl()).isEqualTo(Duration.ofSeconds(30));
            assertThat(properties.cors().allowedOrigins()).isEqualTo(List.of("https://a.example", "https://b.example"));
        });
    }

    @Test
    void aCacheTtlBelowOneSecondPreventsStartup() {
        runner.withPropertyValues("app.parameters.cache-ttl=0s").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining("cacheTtl");
        });
    }

    @Test
    void aSessionTimeoutOutOfRangePreventsStartup() {
        runner.withPropertyValues("app.session.default-timeout=90d").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining("defaultTimeout");
        });
    }

    @Test
    void anUnparseableDurationPreventsStartup() {
        runner.withPropertyValues("app.parameters.cache-ttl=bientot").run(context -> assertThat(context).hasFailed());
    }
}
