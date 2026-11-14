package com.example.userprojectjdbcsqlserver.security;

import com.example.userprojectjdbcsqlserver.entity.Profil;
import com.example.userprojectjdbcsqlserver.exception.FunctionAccessDeniedException;
import com.example.userprojectjdbcsqlserver.repository.ProfilRepository;
import com.example.userprojectjdbcsqlserver.service.ParameterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

// Verifie, pour chaque ecran protege par @RequiresFunction, que la fonction appartient au profil de l'utilisateur.
// S'execute dans le DispatcherServlet AVANT l'appel du controleur, donc avant le proxy de securite des methodes
// (@PreAuthorize) : un refus de fonction est reconnu et journalise a part, un refus de permission reste un 403 simple.
// L'ApiException lancee ici est traitee par GlobalExceptionHandler (la locale est deja peuplee a ce stade).
@Slf4j
@Component
public class FunctionAccessInterceptor implements HandlerInterceptor {

    private final ProfilRepository profilRepository;
    private final ParameterService parameterService;

    public FunctionAccessInterceptor(ProfilRepository profilRepository, ParameterService parameterService) {
        this.profilRepository = profilRepository;
        this.parameterService = parameterService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Preflight CORS, ressources statiques, /error... ne sont pas des HandlerMethod.
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        List<String> required = requiredFunctions(handlerMethod);
        if (required.isEmpty()) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Non authentifie : c'est la chaine de securite (401) qui en decide, pas ce controle.
        if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUser principal)) {
            return true;
        }

        // Module desactive par un parametre interne (module.<fonction>.enabled = false) : la fonction est refusee a tous,
        // quel que soit le profil, sans toucher aux profils. Lu a chaque requete (cache du service de parametres).
        List<String> enabled = required.stream().filter(parameterService::isModuleEnabled).toList();
        if (enabled.isEmpty()) {
            log.warn("Acces refuse : module desactive {} : utilisateur '{}', {} {}, ip {}",
                    required, principal.getUsername(), request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
            throw new FunctionAccessDeniedException();
        }

        // principal.getUser() vient d'etre recharge par PermissionRefreshFilter : le profil est celui d'AUJOURD'HUI.
        Profil profil = principal.getUser().getProfil();
        if (profil != null && profilRepository.hasAnyFunction(profil.getId(), enabled)) {
            return true;
        }

        log.warn("Acces refuse a la fonction {} : utilisateur '{}' (profil '{}'), {} {}, ip {}",
                required, principal.getUsername(), profil != null ? profil.getCode() : "aucun",
                request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
        throw new FunctionAccessDeniedException();
    }

    // Methode d'abord (elle remplace la classe), puis classe. @FunctionExempt -> liste vide (aucune exigence).
    private static List<String> requiredFunctions(HandlerMethod handlerMethod) {
        RequiresFunction onMethod = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequiresFunction.class);
        if (onMethod != null) {
            return List.of(onMethod.value());
        }
        if (AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), FunctionExempt.class)) {
            return List.of();
        }
        Class<?> controller = handlerMethod.getBeanType();
        RequiresFunction onClass = AnnotatedElementUtils.findMergedAnnotation(controller, RequiresFunction.class);
        if (onClass != null) {
            return List.of(onClass.value());
        }
        return List.of();
    }
}
