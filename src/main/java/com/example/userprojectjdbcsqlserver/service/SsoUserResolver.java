package com.example.userprojectjdbcsqlserver.service;

import com.example.userprojectjdbcsqlserver.config.AppProperties;
import com.example.userprojectjdbcsqlserver.entity.User;
import com.example.userprojectjdbcsqlserver.repository.UserIdentityRepository;
import com.example.userprojectjdbcsqlserver.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Rapproche un compte OIDC (Google, Microsoft) d'un utilisateur LOCAL existant. Aucun compte n'est cree : un
 * administrateur cree l'utilisateur, le fournisseur ne fait que l'authentifier.
 *
 * 1. (provider, subject) deja lie : utilisateur retrouve par l'identifiant STABLE du fournisseur, l'e-mail n'intervient plus.
 * 2. Sinon (1re connexion) : rapprochement par e-mail VERIFIE, puis le lien est enregistre.
 *    - Google : "sub" ; "email_verified" doit etre vrai.
 *    - Microsoft : "oid" (repli "sub") ; "tid" doit etre le tenant configure ; e-mail = "email" sinon "preferred_username".
 *      L'e-mail Microsoft n'est pas garanti verifie : c'est le tenant unique (annuaire de l'organisation) qui le garantit.
 * Un compte verrouille est refuse. Le mot de passe expire et le compteur d'echecs ne s'appliquent pas (aucun mot de
 * passe n'est saisi).
 */
@Slf4j
@Service
public class SsoUserResolver {

    public static final String GOOGLE = "google";
    public static final String MICROSOFT = "microsoft";

    public static final String CODE_USER_UNKNOWN = "SSO_USER_UNKNOWN";
    public static final String CODE_ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String CODE_FAILED = "SSO_FAILED";

    /** Echec de rapprochement : le code (un des trois ci-dessus) est transmis au frontend, qui le traduit. */
    public static class SsoLoginException extends RuntimeException {
        private final String code;

        public SsoLoginException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    private final UserRepository userRepository;
    private final UserIdentityRepository identityRepository;
    private final String microsoftTenantId;

    public SsoUserResolver(UserRepository userRepository, UserIdentityRepository identityRepository,
                           AppProperties appProperties) {
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.microsoftTenantId = appProperties.sso().microsoft().tenantId();
    }

    /** @param provider identifiant de l'inscription ("google" ou "microsoft") ; @param claims claims de l'id_token. */
    public User resolve(String provider, Map<String, Object> claims) {
        String subject;
        String email;
        boolean emailVerified;
        if (GOOGLE.equals(provider)) {
            subject = text(claims, "sub");
            email = text(claims, "email");
            emailVerified = Boolean.parseBoolean(String.valueOf(claims.get("email_verified")));
        } else if (MICROSOFT.equals(provider)) {
            String tid = text(claims, "tid");
            if (microsoftTenantId == null || microsoftTenantId.isBlank() || !microsoftTenantId.equalsIgnoreCase(tid)) {
                throw new SsoLoginException(CODE_FAILED, "tenant Microsoft inattendu : " + tid);
            }
            subject = text(claims, "oid") != null ? text(claims, "oid") : text(claims, "sub");
            email = text(claims, "email") != null ? text(claims, "email") : text(claims, "preferred_username");
            // Le tenant unique tient lieu de verification de l'e-mail (voir la description de la classe).
            emailVerified = true;
        } else {
            throw new SsoLoginException(CODE_FAILED, "fournisseur inconnu : " + provider);
        }
        if (subject == null) {
            throw new SsoLoginException(CODE_FAILED, "identifiant (sub/oid) absent du jeton");
        }

        // 1. Identite deja liee.
        var linkedUserId = identityRepository.findUserId(provider, subject);
        if (linkedUserId.isPresent()) {
            User user = userRepository.findById(linkedUserId.get())
                    .orElseThrow(() -> new SsoLoginException(CODE_FAILED, "utilisateur lie introuvable"));
            rejectIfLocked(user, provider);
            identityRepository.touchLastLogin(provider, subject);
            return user;
        }

        // 2. Premiere connexion : e-mail verifie obligatoire.
        if (!emailVerified || email == null || !email.contains("@")) {
            throw new SsoLoginException(CODE_FAILED, "e-mail absent ou non verifie chez le fournisseur " + provider);
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new SsoLoginException(CODE_USER_UNKNOWN, "aucun utilisateur local pour " + email));
        rejectIfLocked(user, provider);
        // Cet utilisateur est deja lie a un AUTRE compte de ce fournisseur : l'adresse a peut-etre ete reattribuee, on
        // refuse plutot que de laisser un nouveau titulaire de l'adresse prendre le compte.
        if (identityRepository.existsForUser(user.getId(), provider)) {
            throw new SsoLoginException(CODE_FAILED, "utilisateur '" + user.getEmail()
                    + "' deja lie a un autre compte " + provider);
        }
        identityRepository.link(user.getId(), provider, subject, email);
        log.info("Compte {} lie a l'utilisateur '{}' (premiere connexion SSO)", provider, user.getEmail());
        return user;
    }

    private static void rejectIfLocked(User user, String provider) {
        if (user.isLocked()) {
            throw new SsoLoginException(CODE_ACCOUNT_LOCKED, "compte verrouille : '" + user.getEmail() + "' (" + provider + ")");
        }
    }

    private static String text(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        return value == null || value.toString().isBlank() ? null : value.toString();
    }
}
