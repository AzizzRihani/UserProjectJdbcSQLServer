package com.example.userprojectjdbcsqlserver.controller;

import com.example.userprojectjdbcsqlserver.dto.LoginRequest;
import com.example.userprojectjdbcsqlserver.dto.LoginResponse;
import com.example.userprojectjdbcsqlserver.dto.SessionResponse;
import com.example.userprojectjdbcsqlserver.security.SecurityUser;
import com.example.userprojectjdbcsqlserver.service.AuthService;
import com.example.userprojectjdbcsqlserver.security.FunctionExempt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentification")
@FunctionExempt
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authentification par email/mot de passe. Cree une session serveur (Spring Session JDBC) et pose un cookie de session httpOnly/secure")
    public LoginResponse login(@RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return authService.login(request, httpRequest.getHeader("User-Agent"), httpRequest.getRemoteAddr(), httpRequest, httpResponse);
    }

    @GetMapping("/me")
    @Operation(summary = "Retourne l'utilisateur de la session courante (401 si non authentifie) - utilise pour restaurer la session au demarrage du frontend")
    public LoginResponse me(@AuthenticationPrincipal SecurityUser currentUser) {
        return authService.me(currentUser);
    }

    @GetMapping("/sessions")
    @Operation(summary = "Liste les sessions actives de l'utilisateur connecte")
    public List<SessionResponse> sessions(@AuthenticationPrincipal SecurityUser currentUser) {
        return authService.listSessions(currentUser.getUsername());
    }

    @DeleteMapping("/sessions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoque une session precise de l'utilisateur connecte (deconnexion d'un autre appareil)")
    public void revokeSession(@PathVariable String id, @AuthenticationPrincipal SecurityUser currentUser) {
        authService.revokeSession(currentUser.getUsername(), id);
    }
}
