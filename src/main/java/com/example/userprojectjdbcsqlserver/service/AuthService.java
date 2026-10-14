package com.example.userprojectjdbcsqlserver.service;

import com.example.userprojectjdbcsqlserver.config.SecurityConfig;
import com.example.userprojectjdbcsqlserver.dto.LoginRequest;
import com.example.userprojectjdbcsqlserver.dto.LoginResponse;
import com.example.userprojectjdbcsqlserver.dto.RoleSummary;
import com.example.userprojectjdbcsqlserver.dto.SessionResponse;
import com.example.userprojectjdbcsqlserver.entity.Role;
import com.example.userprojectjdbcsqlserver.entity.User;
import com.example.userprojectjdbcsqlserver.exception.SelfManagementForbiddenException;
import com.example.userprojectjdbcsqlserver.exception.SessionNotFoundException;
import com.example.userprojectjdbcsqlserver.exception.UserNotFoundException;
import com.example.userprojectjdbcsqlserver.repository.RoleRepository;
import com.example.userprojectjdbcsqlserver.repository.UserRepository;
import com.example.userprojectjdbcsqlserver.security.SecurityUser;
import com.example.userprojectjdbcsqlserver.security.SessionMetadataStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthenticationManager authenticationManager;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextRepository securityContextRepository;
    private final SessionRegistry sessionRegistry;
    private final SessionMetadataStore sessionMetadataStore;

    public AuthService(UserRepository userRepository,
                        RoleRepository roleRepository,
                        AuthenticationManager authenticationManager,
                        SessionAuthenticationStrategy sessionAuthenticationStrategy,
                        SecurityContextRepository securityContextRepository,
                        SessionRegistry sessionRegistry,
                        SessionMetadataStore sessionMetadataStore) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.authenticationManager = authenticationManager;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.securityContextRepository = securityContextRepository;
        this.sessionRegistry = sessionRegistry;
        this.sessionMetadataStore = sessionMetadataStore;
    }

    public LoginResponse    login(LoginRequest request, String userAgent, String ipAddress,
                                HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication authRequest = new UsernamePasswordAuthenticationToken(request.getEmail(), request.getMotDePasse());
        Authentication authResult = authenticationManager.authenticate(authRequest);

        // Cree/enregistre la HttpSession dans le SessionRegistry (login = endpoint JSON custom,
        // pas UsernamePasswordAuthenticationFilter, donc ce cablage n'est pas automatique).
        sessionAuthenticationStrategy.onAuthentication(authResult, httpRequest, httpResponse);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authResult);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        User user = ((SecurityUser) authResult.getPrincipal()).getUser();

        HttpSession session = httpRequest.getSession();
        System.out.println(session.getId());
        sessionMetadataStore.put(session.getId(), userAgent, ipAddress);

        log.info("Connexion reussie : utilisateur '{}' (roles {})", user.getEmail(),
                user.getRoles().stream().map(Role::getCode).collect(Collectors.joining("+")));
        return toLoginResponse(user);
    }

    public LoginResponse me(SecurityUser currentUser) {
        return toLoginResponse(currentUser.getUser());
    }

    public List<SessionResponse> listSessions(String email) {
        SecurityUser principal = loadPrincipal(email);
        return sessionRegistry.getAllSessions(principal, false).stream()
                .map(this::toSessionResponse)
                .toList();
    }

    public void revokeSession(String email, String sessionId) {
        SessionInformation info = sessionRegistry.getSessionInformation(sessionId);
        if (info == null || info.isExpired() || !ownedBy(info, email)) {
            // 404 (jamais 403) : ne pas reveler l'existence d'une session appartenant a un autre utilisateur.
            throw new SessionNotFoundException(sessionId);
        }
        info.expireNow();
        log.info("Utilisateur '{}' a revoque manuellement la session id={}", email, sessionId);
    }

    public void revokeAllAccessForUser(Long adminId, Long targetUserId) {
        if (targetUserId.equals(adminId)) {
            throw new SelfManagementForbiddenException();
        }
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));

        List<SessionInformation> sessions = sessionRegistry.getAllSessions(new SecurityUser(target), false);
        sessions.forEach(SessionInformation::expireNow);

        log.info("Admin id={} a revoque instantanement {} session(s) de l'utilisateur '{}' (id={})",
                adminId, sessions.size(), target.getEmail(), targetUserId);
    }

    private SecurityUser loadPrincipal(String email) {
        return userRepository.findByEmail(email)
                .map(SecurityUser::new)
                .orElseThrow(() -> new IllegalStateException("Utilisateur authentifie introuvable : " + email));
    }

    private boolean ownedBy(SessionInformation info, String email) {
        return info.getPrincipal() instanceof SecurityUser securityUser && securityUser.getUsername().equals(email);
    }

    private LoginResponse toLoginResponse(User user) {
        // Union des permissions de TOUS les roles de l'utilisateur.
        List<String> permissions = roleRepository.findPermissionCodesByRoleIds(roleIds(user))
                .stream().sorted().toList();
        return LoginResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .roles(RoleSummary.fromEntities(user.getRoles()))
                .permissions(permissions)
                .build();
    }

    private static List<Long> roleIds(User user) {
        return user.getRoles().stream().map(Role::getId).toList();
    }

    private SessionResponse toSessionResponse(SessionInformation info) {
        SessionMetadataStore.SessionMetadata metadata = sessionMetadataStore.get(info.getSessionId());
        Instant lastRequest = info.getLastRequest().toInstant();
        return SessionResponse.builder()
                .id(info.getSessionId())
                .dateCreation(metadata != null ? metadata.creationTime() : lastRequest)
                .dateExpiration(lastRequest.plus(SecurityConfig.SESSION_TIMEOUT))
                .userAgent(metadata != null ? metadata.userAgent() : null)
                .adresseIp(metadata != null ? metadata.ipAddress() : null)
                .build();
    }
}
