package com.example.userprojectjdbcsqlserver.controller;

import com.example.userprojectjdbcsqlserver.dto.UserCreateRequest;
import com.example.userprojectjdbcsqlserver.dto.UserResponse;
import com.example.userprojectjdbcsqlserver.security.SecurityUser;
import com.example.userprojectjdbcsqlserver.service.AuthService;
import com.example.userprojectjdbcsqlserver.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@Tag(name = "Utilisateurs")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping
    //authentication.getAuthorities().contains("USER_CREATE")
    @PreAuthorize("hasAuthority('USER_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Creer un utilisateur")
    public UserResponse create(@RequestBody UserCreateRequest request, @AuthenticationPrincipal SecurityUser currentUser) {
        return userService.create(request, currentUser.getUsername());
    }

    @GetMapping
    @Operation(summary = "Lister tous les utilisateurs (tout utilisateur connecte)")
    public List<UserResponse> findAll(@AuthenticationPrincipal SecurityUser currentUser) {
        return userService.findAll(currentUser.getUsername());
    }

    @GetMapping("/page")
    @Operation(summary = "Lister les utilisateurs (pagine)")
    public Page<UserResponse> findAllPaged(@PageableDefault(size = 10, sort = "nom") Pageable pageable,
                                            @AuthenticationPrincipal SecurityUser currentUser) {
        return userService.getAllPaginated(pageable, currentUser.getUsername());
    }

    @GetMapping("/me")
    @Operation(summary = "Consulter son propre profil")
    public UserResponse me(@AuthenticationPrincipal SecurityUser currentUser) {
        return userService.me(currentUser.getId(), currentUser.getUsername());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter le detail d'un utilisateur")
    public UserResponse findById(@PathVariable Long id, @AuthenticationPrincipal SecurityUser currentUser) {
        return userService.findById(id, currentUser.getUsername());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @Operation(summary = "Modifier un utilisateur (sauf sur son propre compte)")
    public UserResponse update(@PathVariable Long id,
                                @RequestBody UserCreateRequest request,
                                @AuthenticationPrincipal SecurityUser currentUser) {
        return userService.update(id, request, currentUser.getId(), currentUser.getUsername());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer un utilisateur (sauf sur son propre compte)")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal SecurityUser currentUser) {
        userService.delete(id, currentUser.getId(), currentUser.getUsername());
    }

    @PostMapping("/{id}/revoke-access")
    @PreAuthorize("hasAuthority('USER_REVOKE_ACCESS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoque immediatement toutes les sessions actives d'un utilisateur (sauf sur son propre compte)")
    public void revokeAccess(@PathVariable Long id, @AuthenticationPrincipal SecurityUser currentUser) {
        authService.revokeAllAccessForUser(currentUser.getId(), id);
    }
}
