package com.example.userprojectjdbcsqlserver.service;

import com.example.userprojectjdbcsqlserver.config.AppProperties;
import com.example.userprojectjdbcsqlserver.entity.User;
import com.example.userprojectjdbcsqlserver.repository.UserIdentityRepository;
import com.example.userprojectjdbcsqlserver.repository.UserRepository;
import com.example.userprojectjdbcsqlserver.service.SsoUserResolver.SsoLoginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Rapprochement compte OIDC -> utilisateur local : aucune base, depots simules en memoire. */
class SsoUserResolverTest {

    private static final String TENANT = "11111111-2222-3333-4444-555555555555";

    private final UserRepository users = mock(UserRepository.class);
    private final FakeIdentities identities = new FakeIdentities();
    private SsoUserResolver resolver;

    private final User alice = User.builder().id(1L).email("alice@example.com").build();

    @BeforeEach
    void setUp() {
        when(users.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
        when(users.findById(1L)).thenReturn(Optional.of(alice));
        when(users.findByEmail("inconnu@example.com")).thenReturn(Optional.empty());
        resolver = new SsoUserResolver(users, identities, properties());
    }

    private static AppProperties properties() {
        return new AppProperties(
                new AppProperties.Session(Duration.ofDays(3)),
                new AppProperties.Cors(List.of("https://localhost:4200")),
                new AppProperties.Parameters(Duration.ofMinutes(5)),
                new AppProperties.Sso(true, "https://localhost:4200",
                        new AppProperties.Sso.Google("gid", "gsecret"),
                        new AppProperties.Sso.Microsoft(TENANT, "mid", "msecret", "https://login.microsoftonline.com")));
    }

    private static Map<String, Object> google(String sub, String email, Object verified) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", sub);
        claims.put("email", email);
        claims.put("email_verified", verified);
        return claims;
    }

    private static Map<String, Object> microsoft(String oid, String tid, String email, String preferredUsername) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "pairwise-" + oid);
        claims.put("oid", oid);
        claims.put("tid", tid);
        if (email != null) {
            claims.put("email", email);
        }
        if (preferredUsername != null) {
            claims.put("preferred_username", preferredUsername);
        }
        return claims;
    }

    private static String codeOf(Throwable t) {
        return ((SsoLoginException) t).getCode();
    }

    @Test
    void firstGoogleLoginLinksTheUserByVerifiedEmail() {
        User resolved = resolver.resolve("google", google("g-1", "alice@example.com", true));

        assertThat(resolved).isSameAs(alice);
        assertThat(identities.findUserId("google", "g-1")).contains(1L);
    }

    @Test
    void secondLoginUsesTheLinkEvenIfTheEmailChanged() {
        resolver.resolve("google", google("g-1", "alice@example.com", true));

        // L'adresse du compte Google a change : seule l'identite stable (sub) compte desormais.
        User resolved = resolver.resolve("google", google("g-1", "autre.adresse@gmail.com", true));

        assertThat(resolved).isSameAs(alice);
    }

    @Test
    void unverifiedGoogleEmailIsRefusedOnFirstLogin() {
        assertThatThrownBy(() -> resolver.resolve("google", google("g-1", "alice@example.com", false)))
                .isInstanceOf(SsoLoginException.class)
                .satisfies(t -> assertThat(codeOf(t)).isEqualTo("SSO_FAILED"));
        assertThat(identities.findUserId("google", "g-1")).isEmpty();
    }

    @Test
    void missingEmailVerifiedClaimIsTreatedAsUnverified() {
        Map<String, Object> claims = google("g-1", "alice@example.com", null);
        claims.remove("email_verified");

        assertThatThrownBy(() -> resolver.resolve("google", claims))
                .satisfies(t -> assertThat(codeOf(t)).isEqualTo("SSO_FAILED"));
    }

    @Test
    void unknownLocalUserIsRefused() {
        assertThatThrownBy(() -> resolver.resolve("google", google("g-9", "inconnu@example.com", true)))
                .satisfies(t -> assertThat(codeOf(t)).isEqualTo("SSO_USER_UNKNOWN"));
        assertThat(identities.findUserId("google", "g-9")).isEmpty();
    }

    @Test
    void lockedAccountIsRefusedOnFirstLoginAndLaterLogins() {
        alice.setLocked(true);

        assertThatThrownBy(() -> resolver.resolve("google", google("g-1", "alice@example.com", true)))
                .satisfies(t -> assertThat(codeOf(t)).isEqualTo("ACCOUNT_LOCKED"));
        assertThat(identities.findUserId("google", "g-1")).isEmpty();

        alice.setLocked(false);
        resolver.resolve("google", google("g-1", "alice@example.com", true));
        alice.setLocked(true);

        assertThatThrownBy(() -> resolver.resolve("google", google("g-1", "alice@example.com", true)))
                .satisfies(t -> assertThat(codeOf(t)).isEqualTo("ACCOUNT_LOCKED"));
    }

    @Test
    void aSecondGoogleAccountWithTheSameEmailCannotTakeOverALinkedUser() {
        resolver.resolve("google", google("g-1", "alice@example.com", true));

        // L'adresse a ete reattribuee a un autre compte Google (autre sub) : refus, pas de prise de compte.
        assertThatThrownBy(() -> resolver.resolve("google", google("g-2", "alice@example.com", true)))
                .satisfies(t -> assertThat(codeOf(t)).isEqualTo("SSO_FAILED"));
        assertThat(identities.findUserId("google", "g-2")).isEmpty();
    }

    @Test
    void microsoftLinksOnOidAndUsesEmailClaim() {
        User resolved = resolver.resolve("microsoft", microsoft("oid-1", TENANT, "alice@example.com", null));

        assertThat(resolved).isSameAs(alice);
        assertThat(identities.findUserId("microsoft", "oid-1")).contains(1L);
    }

    @Test
    void microsoftFallsBackToPreferredUsernameWhenEmailClaimIsMissing() {
        User resolved = resolver.resolve("microsoft", microsoft("oid-1", TENANT, null, "alice@example.com"));

        assertThat(resolved).isSameAs(alice);
    }

    @Test
    void microsoftTenantIsCheckedCaseInsensitively() {
        User resolved = resolver.resolve("microsoft",
                microsoft("oid-1", TENANT.toUpperCase(), "alice@example.com", null));

        assertThat(resolved).isSameAs(alice);
    }

    @Test
    void microsoftFromAnotherTenantIsRefused() {
        assertThatThrownBy(() -> resolver.resolve("microsoft",
                microsoft("oid-1", "99999999-0000-0000-0000-000000000000", "alice@example.com", null)))
                .satisfies(t -> assertThat(codeOf(t)).isEqualTo("SSO_FAILED"));
        assertThat(identities.findUserId("microsoft", "oid-1")).isEmpty();
    }

    @Test
    void microsoftWithoutTenantClaimIsRefused() {
        assertThatThrownBy(() -> resolver.resolve("microsoft", microsoft("oid-1", null, "alice@example.com", null)))
                .satisfies(t -> assertThat(codeOf(t)).isEqualTo("SSO_FAILED"));
    }

    @Test
    void microsoftWithoutAnyEmailIsRefusedOnFirstLogin() {
        assertThatThrownBy(() -> resolver.resolve("microsoft", microsoft("oid-1", TENANT, null, null)))
                .satisfies(t -> assertThat(codeOf(t)).isEqualTo("SSO_FAILED"));
    }

    @Test
    void tokenWithoutSubjectIsRefused() {
        Map<String, Object> claims = google("g-1", "alice@example.com", true);
        claims.remove("sub");

        assertThatThrownBy(() -> resolver.resolve("google", claims))
                .satisfies(t -> assertThat(codeOf(t)).isEqualTo("SSO_FAILED"));
    }

    @Test
    void unknownProviderIsRefused() {
        assertThatThrownBy(() -> resolver.resolve("github", google("x", "alice@example.com", true)))
                .satisfies(t -> assertThat(codeOf(t)).isEqualTo("SSO_FAILED"));
    }

    @Test
    void identitiesOfDifferentProvidersAreIndependent() {
        resolver.resolve("google", google("same-id", "alice@example.com", true));

        // Meme valeur de sujet chez un autre fournisseur : ce n'est pas la meme identite.
        assertThat(identities.findUserId("microsoft", "same-id")).isEmpty();
        resolver.resolve("microsoft", microsoft("same-id", TENANT, "alice@example.com", null));
        assertThat(identities.findUserId("microsoft", "same-id")).contains(1L);
    }

    /** Depot en memoire : (provider, subject) -> utilisateur. */
    private static class FakeIdentities implements UserIdentityRepository {
        private final Map<String, Long> byIdentity = new HashMap<>();

        @Override
        public Optional<Long> findUserId(String provider, String subject) {
            return Optional.ofNullable(byIdentity.get(provider + "|" + subject));
        }

        @Override
        public boolean existsForUser(Long userId, String provider) {
            return byIdentity.entrySet().stream()
                    .anyMatch(e -> e.getKey().startsWith(provider + "|") && e.getValue().equals(userId));
        }

        @Override
        public void link(Long userId, String provider, String subject, String email) {
            byIdentity.put(provider + "|" + subject, userId);
        }

        @Override
        public void touchLastLogin(String provider, String subject) {
        }
    }
}
