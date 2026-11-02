package com.example.userprojectjdbcsqlserver.controller;

import com.example.userprojectjdbcsqlserver.dto.FonctionResponse;
import com.example.userprojectjdbcsqlserver.repository.FonctionRepository;
import com.example.userprojectjdbcsqlserver.security.RequiresFunction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/fonctions")
@Tag(name = "Fonctions")
@RequiresFunction("PROFILS")
@PreAuthorize("hasAuthority('PROFILE_MANAGE')")
public class FonctionController {

    private final FonctionRepository fonctionRepository;

    public FonctionController(FonctionRepository fonctionRepository) {
        this.fonctionRepository = fonctionRepository;
    }

    @GetMapping
    @Operation(summary = "Lister le catalogue complet des fonctions (arbre des menus, liste plate)")
    public List<FonctionResponse> findAll() {
        return FonctionResponse.fromEntities(fonctionRepository.findAll());
    }
}