package com.example.userprojectjdbcsqlserver.security;

import com.example.userprojectjdbcsqlserver.entity.Role;
import com.example.userprojectjdbcsqlserver.entity.User;
import com.example.userprojectjdbcsqlserver.repository.RoleRepository;
import com.example.userprojectjdbcsqlserver.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

// L'Authentication stockee en session est une copie prise au login : sans ce filtre, un changement
// de role ou de permissions fait par un admin ne s'appliquerait qu'apres reconnexion de l'utilisateur.
// Ici on la reconstruit a chaque requete depuis la base (2 petites requetes), donc les droits
// appliques par @PreAuthorize sont toujours les droits actuels.
// Volontairement instancie a la main dans SecurityConfig (pas un @Component) pour n'etre enregistre
// que dans la chaine Spring Security, apres le chargement du SecurityContext et avant l'autorisation.
public class PermissionRefreshFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public PermissionRefreshFilter(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication current = context.getAuthentication();

        if (current != null && current.isAuthenticated() && current.getPrincipal() instanceof SecurityUser principal) {
            Optional<User> fresh = userRepository.findById(principal.getId());
            if (fresh.isEmpty()) {
                // Utilisateur supprime depuis son login : sa session ne doit plus donner aucun acces.
                SecurityContextHolder.clearContext();
            } else {
                Set<String> codes = roleRepository.findPermissionCodesByRoleIds(
                        fresh.get().getRoles().stream().map(Role::getId).toList());
                SecurityUser refreshed = new SecurityUser(fresh.get(), codes);
                UsernamePasswordAuthenticationToken updated =
                        UsernamePasswordAuthenticationToken.authenticated(refreshed, null, refreshed.getAuthorities());
                updated.setDetails(current.getDetails());
                context.setAuthentication(updated);
            }
        }

        filterChain.doFilter(request, response);
    }
}
