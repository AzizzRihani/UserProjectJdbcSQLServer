package com.example.userprojectjdbcsqlserver.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Configuration SSO : desactivee par defaut (aucun bean, aucune variable requise), et un demarrage incoherent est refuse
 * avec un message explicite. Aucune base ni serveur, aucun appel reseau (les points d'acces sont calcules).
 */
class SsoConfigTest {

    @Configuration
    @EnableConfigurationProperties(AppProperties.class)
    static class Props {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Props.class, SsoConfig.class);

    @Test
    void disabledByDefaultCreatesNoRegistrationAndNeedsNoVariable() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(InMemoryClientRegistrationRepository.class);
        });
    }

    @Test
    void enabledWithoutAnyProviderPreventsStartupWithAClearMessage() {
        runner.withPropertyValues("app.sso.enabled=true").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining("aucun fournisseur n'est configure");
        });
    }

    @Test
    void aProviderWithoutClientIdIsIgnored() {
        runner.withPropertyValues("app.sso.enabled=true",
                "app.sso.google.client-id=gid", "app.sso.google.client-secret=gsecret",
                "app.sso.microsoft.client-id=", "app.sso.microsoft.tenant-id=").run(context -> {
            assertThat(context).hasNotFailed();
            InMemoryClientRegistrationRepository repository = context.getBean(InMemoryClientRegistrationRepository.class);
            assertThat(repository.findByRegistrationId("google")).isNotNull();
            assertThat(repository.findByRegistrationId("microsoft")).isNull();
        });
    }

    @Test
    void aClientIdWithoutSecretPreventsStartup() {
        runner.withPropertyValues("app.sso.enabled=true", "app.sso.google.client-id=gid").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining("GOOGLE_CLIENT_SECRET");
        });
    }

    @Test
    void microsoftWithoutTenantPreventsStartup() {
        runner.withPropertyValues("app.sso.enabled=true",
                "app.sso.microsoft.client-id=mid", "app.sso.microsoft.client-secret=msecret").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining("MICROSOFT_TENANT_ID");
        });
    }

    @Test
    void microsoftEndpointsAreComputedFromAuthorityAndTenantWithoutNetworkCall() {
        runner.withPropertyValues("app.sso.enabled=true",
                "app.sso.microsoft.client-id=mid", "app.sso.microsoft.client-secret=msecret",
                "app.sso.microsoft.tenant-id=AAAA-BBBB", "app.sso.microsoft.authority=https://idp.test/").run(context -> {
            assertThat(context).hasNotFailed();
            ClientRegistration microsoft = context.getBean(InMemoryClientRegistrationRepository.class)
                    .findByRegistrationId("microsoft");
            // Tenant normalise en minuscules (le claim "iss" des jetons Entra l'est), pas de double barre.
            assertThat(microsoft.getProviderDetails().getIssuerUri()).isEqualTo("https://idp.test/aaaa-bbbb/v2.0");
            assertThat(microsoft.getProviderDetails().getAuthorizationUri())
                    .isEqualTo("https://idp.test/aaaa-bbbb/oauth2/v2.0/authorize");
            assertThat(microsoft.getProviderDetails().getTokenUri())
                    .isEqualTo("https://idp.test/aaaa-bbbb/oauth2/v2.0/token");
            assertThat(microsoft.getProviderDetails().getJwkSetUri())
                    .isEqualTo("https://idp.test/aaaa-bbbb/discovery/v2.0/keys");
            assertThat(microsoft.getScopes()).contains("openid", "email");
        });
    }
}
