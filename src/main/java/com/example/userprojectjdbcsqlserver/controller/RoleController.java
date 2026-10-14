package com.example.userprojectjdbcsqlserver.controller;

import com.example.userprojectjdbcsqlserver.dto.RoleCreateRequest;
import com.example.userprojectjdbcsqlserver.dto.RolePermissionsRequest;
import com.example.userprojectjdbcsqlserver.dto.RoleResponse;
import com.example.userprojectjdbcsqlserver.dto.RoleUpdateRequest;
import com.example.userprojectjdbcsqlserver.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
@Tag(name = "Roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    // Pas de @PreAuthorize ici (contrairement au reste du controleur) : la liste des roles sert
    // aussi au selecteur de role dans le formulaire utilisateur, utilise par quiconque a
    // USER_CREATE/USER_UPDATE - pas seulement les detenteurs de ROLE_MANAGE. Les noms/libelles de
    // roles ne sont pas sensibles ; seule la gestion (creer/modifier/supprimer/assigner des
    // permissions) l'est.
    @GetMapping
    @Operation(summary = "Lister tous les roles avec leurs permissions")
    public List<RoleResponse> findAll() {
        return roleService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter le detail d'un role")
    public RoleResponse findById(@PathVariable Long id) {
        return roleService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Creer un role")
    public RoleResponse create(@RequestBody RoleCreateRequest request) {
        return roleService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    @Operation(summary = "Modifier le libelle d'un role")
    public RoleResponse update(@PathVariable Long id, @RequestBody RoleUpdateRequest request) {
        return roleService.update(id, request);
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    @Operation(summary = "Remplacer l'ensemble des permissions d'un role")
    public RoleResponse replacePermissions(@PathVariable Long id, @RequestBody RolePermissionsRequest request) {
        return roleService.replacePermissions(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer un role")
    public void delete(@PathVariable Long id) {
        roleService.delete(id);
    }
}
