package com.example.userprojectjdbcsqlserver.security;

import com.example.userprojectjdbcsqlserver.entity.User;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Map;
import java.util.Set;

/**
 * Principal d'une connexion SSO : un SecurityUser (donc identique au login classique pour tout le reste de
 * l'application : @AuthenticationPrincipal SecurityUser, PermissionRefreshFilter, SessionRegistry) qui est AUSSI un
 * OidcUser, ce que Spring Security exige d'un utilisateur retourne par un OidcUserService.
 */
public class SsoSecurityUser extends SecurityUser implements OidcUser {

    private static final long serialVersionUID = 1L;

    private final OidcIdToken idToken;

    public SsoSecurityUser(User user, Set<String> permissionCodes, OidcIdToken idToken) {
        super(user, permissionCodes);
        this.idToken = idToken;
    }

    @Override
    public Map<String, Object> getClaims() {
        return idToken.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        // Tout ce dont on a besoin est dans l'id_token valide : l'endpoint userinfo n'est pas appele.
        return null;
    }

    @Override
    public OidcIdToken getIdToken() {
        return idToken;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return getClaims();
    }

    @Override
    public String getName() {
        return getUsername();
    }
}
