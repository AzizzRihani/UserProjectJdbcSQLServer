package com.example.userprojectjdbcsqlserver.controller;

import com.example.userprojectjdbcsqlserver.dto.PermissionResponse;
import com.example.userprojectjdbcsqlserver.repository.PermissionRepository;
import com.example.userprojectjdbcsqlserver.security.RequiresFunction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/permissions")
@Tag(name = "Permissions")
@PreAuthorize("hasAuthority('ROLE_MANAGE')")
@RequiresFunction("ROLES")
public class PermissionController {

    private final PermissionRepository permissionRepository;

    public PermissionController(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @GetMapping
    @Operation(summary = "Lister le catalogue complet des permissions, groupees par module")
    public List<PermissionResponse> findAll() {
        return permissionRepository.findAll().stream()
                .map(PermissionResponse::fromEntity)
                .toList();
    }
}
