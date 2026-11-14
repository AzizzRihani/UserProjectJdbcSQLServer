package com.example.userprojectjdbcsqlserver.security;

import com.example.userprojectjdbcsqlserver.entity.Role;
import com.example.userprojectjdbcsqlserver.entity.User;
import com.example.userprojectjdbcsqlserver.repository.RoleRepository;
import com.example.userprojectjdbcsqlserver.repository.UserRepository;
import com.example.userprojectjdbcsqlserver.service.ParameterService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ParameterService parameterService;

    public CustomUserDetailsService(UserRepository userRepository, RoleRepository roleRepository,
                                    ParameterService parameterService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.parameterService = parameterService;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + email));
        // Union des permissions de tous les roles de l'utilisateur.
        return new SecurityUser(user, roleRepository.findPermissionCodesByRoleIds(
                user.getRoles().stream().map(Role::getId).toList()), isPasswordExpired(user));
    }

    // Parametre interne security.passwordMaxAgeDays, relu a chaque connexion (modifiable a chaud) ; 0 = jamais.
    private boolean isPasswordExpired(User user) {
        int maxAgeDays = parameterService.getInt(ParameterService.PASSWORD_MAX_AGE_DAYS);
        return maxAgeDays > 0 && user.getPasswordChangedAt() != null
                && user.getPasswordChangedAt().plus(maxAgeDays, ChronoUnit.DAYS).isBefore(Instant.now());
    }
}
