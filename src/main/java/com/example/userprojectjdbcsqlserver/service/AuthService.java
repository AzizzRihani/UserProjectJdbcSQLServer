package com.example.userprojectjdbcsqlserver.service;

import com.example.userprojectjdbcsqlserver.config.AppProperties;
import com.example.userprojectjdbcsqlserver.dto.LoginRequest;
import com.example.userprojectjdbcsqlserver.dto.FonctionResponse;
import com.example.userprojectjdbcsqlserver.dto.LoginResponse;
import com.example.userprojectjdbcsqlserver.dto.RoleSummary;
import com.example.userprojectjdbcsqlserver.dto.SessionResponse;
import com.example.userprojectjdbcsqlserver.entity.Role;
import com.example.userprojectjdbcsqlserver.entity.User;
import com.example.userprojectjdbcsqlserver.exception.LoginTimestampInvalidException;
import com.example.userprojectjdbcsqlserver.exception.SelfManagementForbiddenException;
import com.example.userprojectjdbcsqlserver.exception.SessionNotFoundException;
import com.example.userprojectjdbcsqlserver.exception.UserNotFoundException;
import com.example.userprojectjdbcsqlserver.repository.ProfilRepository;
import com.example.userprojectjdbcsqlserver.repository.RoleRepository;
import com.example.userprojectjdbcsqlserver.repository.UserRepository;
import com.example.userprojectjdbcsqlserver.security.ClientPasswordHash;
import com.example.userprojectjdbcsqlserver.security.SecurityUser;
import com.example.userprojectjdbcsqlserver.security.SessionMetadataStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProfilRepository profilRepository;
    private final AuthenticationManager authenticationManager;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextRepository securityContextRepository;
    private final SessionRegistry sessionRegistry;
    private final SessionMetadataStore sessionMetadataStore;
    private final ParameterService parameterService;
    private final AppProperties appProperties;

    public AuthService(UserRepository userRepository,
                        RoleRepository roleRepository,
                        ProfilRepository profilRepository,
                        AuthenticationManager authenticationManager,
                        SessionAuthenticationStrategy sessionAuthenticationStrategy,
                        SecurityContextRepository securityContextRepository,
                        SessionRegistry sessionRegistry,
                        SessionMetadataStore sessionMetadataStore,
                        ParameterService parameterService,
                        AppProperties appProperties) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.profilRepository = profilRepository;
        this.authenticationManager = authenticationManager;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.securityContextRepository = securityContextRepository;
        this.sessionRegistry = sessionRegistry;
        this.sessionMetadataStore = sessionMetadataStore;
        this.parameterService = parameterService;
        this.appProperties = appProperties;
    }

    // Ecart d'horloge tolere entre le navigateur et le serveur (meme valeur que le vrai TMS : 120 s).
    // L'horodatage n'est pas lie cryptographiquement au hash : il limite la fenetre de rejeu, rien de plus.
    private static final long MAX_LOGIN_CLOCK_SKEW_MS = 120_000L;

    public LoginResponse    login(LoginRequest request, String userAgent, String ipAddress,
                                HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
/*        if (request.getTimestamp() == null
                || Math.abs(System.currentTimeMillis() - request.getTimestamp()) > MAX_LOGIN_CLOCK_SKEW_MS) {
            throw new LoginTimestampInvalidException();
        }*/
        // Le client envoie un hash Argon2id (64 hexa), jamais le mot de passe en clair.
        ClientPasswordHash.requireValid(request.getMotDePasse());

        Authentication authRequest = new UsernamePasswordAuthenticationToken(request.getEmail(), request.getMotDePasse());
        Authentication authResult;
        try {
            authResult = authenticationManager.authenticate(authRequest);
        } catch (BadCredentialsException ex) {
            // Mauvais mot de passe (ou email inconnu, indiscernable ici) : comptabilise l'echec. Un compte deja
            // verrouille ne passe pas ici (LockedException est levee avant la verification du mot de passe).
            registerFailedLogin(request.getEmail());
            throw ex;
        }

        // Cree/enregistre la HttpSession dans le SessionRegistry (login = endpoint JSON custom,
        // pas UsernamePasswordAuthenticationFilter, donc ce cablage n'est pas automatique).
        sessionAuthenticationStrategy.onAuthentication(authResult, httpRequest, httpResponse);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authResult);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        User user = ((SecurityUser) authResult.getPrincipal()).getUser();

        if (user.getFailedAttempts() > 0) {
            userRepository.resetFailedAttempts(user.getId());
        }

        // Duree d'inactivite = parametre interne security.sessionTimeoutMinutes lu A CE MOMENT : ne s'applique qu'aux
        // sessions creees apres une modification du parametre (les sessions deja ouvertes gardent leur duree).
        Duration sessionTimeout = Duration.ofMinutes(parameterService.getInt(ParameterService.SESSION_TIMEOUT_MINUTES));
        HttpSession session = httpRequest.getSession();
        session.setMaxInactiveInterval((int) sessionTimeout.toSeconds());
        sessionMetadataStore.put(session.getId(), userAgent, ipAddress, sessionTimeout);

        log.info("Connexion reussie : utilisateur '{}' (roles {})", user.getEmail(),
                user.getRoles().stream().map(Role::getCode).collect(Collectors.joining("+")));
        return toLoginResponse(user);
    }

    // Parametre interne security.maxFailedAttempts : au N-ieme echec consecutif le compte est verrouille jusqu'a ce
    // qu'un administrateur le deverrouille (comme DISABLE_USER_AFTER_3 du legacy, mais le seuil est borne et modifiable).
    private void registerFailedLogin(String email) {
        int max = parameterService.getInt(ParameterService.MAX_FAILED_ATTEMPTS);
        if (userRepository.recordFailedLogin(email, max)) {
            log.warn("Compte verrouille apres {} echecs de connexion consecutifs : utilisateur '{}'", max, email);
        }
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
                // Profil absent, invalide ou vide -> liste vide -> menu vide cote frontend.
                .functions(FonctionResponse.fromEntities(
                        profilRepository.findEffectiveFunctions(user.getProfil() != null ? user.getProfil().getId() : null)
                                .stream()
                                // Module desactive (parametre module.<fonction>.enabled = false) : retire du menu.
                                .filter(fonction -> parameterService.isModuleEnabled(fonction.getCode()))
                                .toList()))
                .build();
    }

    /** Libelle d'environnement (parametre interne ui.environmentLabel), public : affiche avant la connexion. */
    public String environmentLabel() {
        return parameterService.get(ParameterService.ENVIRONMENT_LABEL);
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
                // Duree memorisee au login ; repli sur la valeur par defaut d'initialisation si la session est inconnue
                // du store (ex. creee avant un redemarrage).
                .dateExpiration(lastRequest.plus(metadata != null ? metadata.timeout() : appProperties.session().defaultTimeout()))
                .userAgent(metadata != null ? metadata.userAgent() : null)
                .adresseIp(metadata != null ? metadata.ipAddress() : null)
                .build();
    }
}
