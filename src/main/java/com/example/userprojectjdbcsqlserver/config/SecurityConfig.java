package com.example.userprojectjdbcsqlserver.config;

import com.example.userprojectjdbcsqlserver.repository.RoleRepository;
import com.example.userprojectjdbcsqlserver.repository.UserRepository;
import com.example.userprojectjdbcsqlserver.security.CsrfCookieFilter;
import com.example.userprojectjdbcsqlserver.security.PermissionRefreshFilter;
import com.example.userprojectjdbcsqlserver.security.RestAccessDeniedHandler;
import com.example.userprojectjdbcsqlserver.security.RestAuthenticationEntryPoint;
import com.example.userprojectjdbcsqlserver.security.RestLogoutSuccessHandler;
import com.example.userprojectjdbcsqlserver.security.RestSessionInformationExpiredStrategy;
import org.springframework.boot.web.server.Cookie;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.server.servlet.Session;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // Duree d'inactivite avant expiration d'une session ; reutilisee par AuthService pour calculer
    // la date d'expiration affichee dans /auth/sessions (SessionInformation ne l'expose pas).
    public static final Duration SESSION_TIMEOUT = Duration.ofDays(3);

    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;
    private final RestLogoutSuccessHandler restLogoutSuccessHandler;
    private final RestSessionInformationExpiredStrategy restSessionInformationExpiredStrategy;
    private final CsrfCookieFilter csrfCookieFilter;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public SecurityConfig(RestAuthenticationEntryPoint restAuthenticationEntryPoint,
                           RestAccessDeniedHandler restAccessDeniedHandler,
                           RestLogoutSuccessHandler restLogoutSuccessHandler,
                           RestSessionInformationExpiredStrategy restSessionInformationExpiredStrategy,
                           CsrfCookieFilter csrfCookieFilter,
                           UserRepository userRepository,
                           RoleRepository roleRepository) {
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
        this.restAccessDeniedHandler = restAccessDeniedHandler;
        this.restLogoutSuccessHandler = restLogoutSuccessHandler;
        this.restSessionInformationExpiredStrategy = restSessionInformationExpiredStrategy;
        this.csrfCookieFilter = csrfCookieFilter;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // Requis pour que SessionRegistry soit notifie de la creation/destruction des HttpSession.
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    // Sessions en memoire (pas de persistance externe) : SessionRegistryImpl est l'implementation
    // par defaut de Spring Security, tenue a jour via HttpSessionEventPublisher ci-dessus.
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy(SessionRegistry sessionRegistry) {
        return new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(),
                new RegisterSessionAuthenticationStrategy(sessionRegistry)));
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    // Configure directement le conteneur Tomcat plutot que de compter sur les proprietes
    // server.servlet.session.* : Spring Boot 4.1 a deplace/reduit une bonne partie de son
    // autoconfiguration web (deja constate pour spring.session.*), donc on prefere ici un
    // reglage explicite et fiable, quelle que soit l'autoconfiguration effectivement active.
    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> sessionCookieCustomizer() {
        return factory -> {
            Session session = new Session();
            session.setTimeout(SESSION_TIMEOUT);
            Cookie cookie = session.getCookie();
            cookie.setName("SESSION");
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            // frontend et backend sont tous deux en https://localhost (meme scheme + domaine racine) :
            // "same-site" au sens de Chrome malgre des ports differents, donc Lax suffit et est bien
            // envoye sur les appels fetch/XHR de l'app (Lax ne bloque que les requetes cross-site).
            cookie.setSameSite(Cookie.SameSite.LAX);
            cookie.setMaxAge(SESSION_TIMEOUT);
            factory.setSession(session);
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                     SessionAuthenticationStrategy sessionAuthenticationStrategy,
                                                     SecurityContextRepository securityContextRepository,
                                                     SessionRegistry sessionRegistry) throws Exception {
        http
                // Le cookie de session porte desormais l'authentification sur CHAQUE requete: le CSRF redevient
                // necessaire sur toute l'API.
                // Validation cote serveur inchangee (double-submit cookie standard Spring Security) ; cote
                // client, le frontend recupere le token via le header de reponse X-CSRF-TOKEN (CsrfCookieFilter)
                // plutot que via le cookie XSRF-TOKEN : ce cookie appartient a l'origine du backend
                // (localhost:8088), donc document.cookie cote frontend (localhost:4200) ne peut de toute
                // facon jamais le lire, meme scheme identique (isolation par origine, pas par scheme).
                .csrf(csrf -> {
                    CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
                    csrfTokenRepository.setCookieCustomizer(cookie -> cookie.sameSite("Lax").secure(true));
                    csrf.csrfTokenRepository(csrfTokenRepository)
                            .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler());
                })
                // Positionne juste apres CsrfFilter (pas apres BasicAuthenticationFilter) : des filtres
                // comme LogoutFilter court-circuitent la chaine AVANT BasicAuthenticationFilter, donc un
                // positionnement plus tardif laisserait des reponses (ex. /auth/logout) sans header a jour.
                .addFilterAfter(csrfCookieFilter, CsrfFilter.class)
                // Recharge role + permissions depuis la base a chaque requete : un changement fait par un
                // admin s'applique immediatement, sans que l'utilisateur ait a se reconnecter.
                .addFilterAfter(new PermissionRefreshFilter(userRepository, roleRepository), SecurityContextHolderFilter.class)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // API JSON pure : pas de "redirection vers la page demandee apres login" a preparer.
                // Sans ca, le RequestCache par defaut (HttpSessionRequestCache) cree une HttpSession
                // a CHAQUE requete anonyme sur un endpoint protege, juste pour memoriser une requete
                // qui ne sera jamais rejouee.
                .requestCache(cache -> cache.disable())
                .sessionManagement(sess -> sess
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionAuthenticationStrategy(sessionAuthenticationStrategy)
                        .maximumSessions(-1) // illimite : garde le multi-device (plusieurs sessions actives simultanement)
                        .sessionRegistry(sessionRegistry)
                        // Sans ca, une session revoquee (DELETE /auth/sessions/{id}, revoke-access admin)
                        // renvoie par defaut un texte brut "This session has been expired..." avec un
                        // statut 200 (!) sur la requete suivante, au lieu d'un 401 JSON coherent.
                        .expiredSessionStrategy(restSessionInformationExpiredStrategy))
                .securityContext(ctx -> ctx.securityContextRepository(securityContextRepository))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("SESSION")
                        .logoutSuccessHandler(restLogoutSuccessHandler));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("https://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        // Sans ca, le JS ne peut pas lire ce header custom sur la reponse (restriction CORS par defaut).
        configuration.setExposedHeaders(List.of("X-CSRF-TOKEN"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
