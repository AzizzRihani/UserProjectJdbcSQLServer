package com.example.userprojectjdbcsqlserver.controller;

import com.example.userprojectjdbcsqlserver.dto.ProfilRequest;
import com.example.userprojectjdbcsqlserver.dto.ProfilResponse;
import com.example.userprojectjdbcsqlserver.security.RequiresFunction;
import com.example.userprojectjdbcsqlserver.security.SecurityUser;
import com.example.userprojectjdbcsqlserver.service.ProfilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/profils")
@Tag(name = "Profils")
@RequiresFunction("PROFILS")
public class ProfilController {

    private final ProfilService profilService;

    public ProfilController(ProfilService profilService) {
        this.profilService = profilService;
    }

    // Aussi utilise par le formulaire utilisateur (liste deroulante d'affectation) : accepte la fonction
    // UTILISATEURS en plus de PROFILS, et la permission d'affecter un profil en plus de celle de les gerer.
    @GetMapping
    @RequiresFunction({"UTILISATEURS", "PROFILS"})
    @PreAuthorize("hasAnyAuthority('PROFILE_MANAGE', 'USER_ASSIGN_PROFILE')")
    @Operation(summary = "Lister les profils avec leurs fonctions")
    public List<ProfilResponse> findAll() {
        return profilService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PROFILE_MANAGE')")
    @Operation(summary = "Consulter un profil et ses fonctions")
    public ProfilResponse findById(@PathVariable Long id) {
        return profilService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PROFILE_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Creer un profil avec ses fonctions (les parents sont ajoutes automatiquement)")
    public ProfilResponse create(@RequestBody ProfilRequest request, @AuthenticationPrincipal SecurityUser currentUser) {
        return profilService.create(request, currentUser.getUsername());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PROFILE_MANAGE')")
    @Operation(summary = "Modifier un profil et remplacer ses fonctions (profil SUPER_ADMIN non modifiable)")
    public ProfilResponse update(@PathVariable Long id, @RequestBody ProfilRequest request,
                                 @AuthenticationPrincipal SecurityUser currentUser) {
        return profilService.update(id, request, currentUser.getUsername());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PROFILE_MANAGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer un profil (refuse s'il est utilise ; profil SUPER_ADMIN non supprimable)")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal SecurityUser currentUser) {
        profilService.delete(id, currentUser.getUsername());
    }
}
