package com.example.userprojectjdbcsqlserver.security;

import com.example.userprojectjdbcsqlserver.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;

public class SecurityUser implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final User user;
    private final Set<String> permissionCodes;

    // Les deux usages de ce constructeur a un seul argument (AuthService.loadPrincipal et
    // revokeAllAccessForUser) ne servent qu'a l'egalite dans SessionRegistry (par id, cf. equals
    // ci-dessous) - jamais a une verification @PreAuthorize - donc un ensemble de permissions vide
    // leur suffit, pas besoin d'un aller-retour RoleRepository superflu.
    public SecurityUser(User user) {
        this(user, Set.of());
    }

    public SecurityUser(User user, Set<String> permissionCodes) {
        this.user = user;
        this.permissionCodes = permissionCodes;
    }

    public User getUser() {
        return user;
    }

    public Long getId() {
        return user.getId();
    }

    // Les autorites sont directement les codes de permission resolus depuis role_permissions
    // (ex. "BANQUE_CREATE") - plus de prefixe "ROLE_" : hasRole(...) a ete entierement remplace
    // par hasAuthority(...) puisque les roles ne sont plus un enum fige que Spring Security peut
    // nommer a l'avance.
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return permissionCodes.stream().map(SimpleGrantedAuthority::new).toList();
    }

    @Override
    public String getPassword() {
        return user.getMotDePasse();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    // Necessaire pour SessionRegistryImpl (session en memoire) : il indexe les sessions par principal
    // dans une Map<Object, ...>, comparee par equals(). Sans ca (egalite par reference par defaut),
    // deux connexions du meme utilisateur (2 appareils, donc 2 instances SecurityUser distinctes,
    // rechargees separement depuis la base a chaque login) ne seraient jamais regroupees ensemble :
    // getAllSessions(unSecurityUser, false) ne retournerait que la session de CETTE instance precise.
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SecurityUser other)) {
            return false;
        }
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getId() != null ? getId().hashCode() : 0;
    }
}
