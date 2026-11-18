package com.example.userprojectjdbcsqlserver.security;

import com.example.userprojectjdbcsqlserver.entity.Role;
import com.example.userprojectjdbcsqlserver.entity.User;
import com.example.userprojectjdbcsqlserver.repository.RoleRepository;
import com.example.userprojectjdbcsqlserver.service.SsoUserResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Appele par Spring APRES validation de l'id_token (signature, issuer, audience, nonce, expiration) : on rapproche
 * alors le compte du fournisseur d'un utilisateur local et on renvoie un SecurityUser (voir SsoSecurityUser).
 * Un refus remonte comme OAuth2AuthenticationException dont le code d'erreur est celui affiche par le frontend.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.sso", name = "enabled", havingValue = "true")
public class SsoOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final SsoUserResolver resolver;
    private final RoleRepository roleRepository;

    public SsoOidcUserService(SsoUserResolver resolver, RoleRepository roleRepository) {
        this.resolver = resolver;
        this.roleRepository = roleRepository;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        String provider = userRequest.getClientRegistration().getRegistrationId();
        OidcIdToken idToken = userRequest.getIdToken();
        try {
            User user = resolver.resolve(provider, idToken.getClaims());
            Set<String> permissions = roleRepository.findPermissionCodesByRoleIds(
                    user.getRoles().stream().map(Role::getId).toList());
            return new SsoSecurityUser(user, permissions, idToken);
        } catch (SsoUserResolver.SsoLoginException ex) {
            throw new OAuth2AuthenticationException(new OAuth2Error(ex.getCode()), ex.getMessage(), ex);
        }
    }
}
