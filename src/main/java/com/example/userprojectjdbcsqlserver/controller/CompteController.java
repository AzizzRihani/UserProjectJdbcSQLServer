package com.example.userprojectjdbcsqlserver.controller;

import com.example.userprojectjdbcsqlserver.dto.CompteRequest;
import com.example.userprojectjdbcsqlserver.dto.CompteResponse;
import com.example.userprojectjdbcsqlserver.service.CompteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comptes")
@Tag(name = "Comptes bancaires")
public class CompteController {

    private final CompteService compteService;

    public CompteController(CompteService compteService) {
        this.compteService = compteService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('COMPTE_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Creer un compte bancaire")
    public CompteResponse create(@RequestBody CompteRequest request) {
        return compteService.create(request);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('COMPTE_READ')")
    @Operation(summary = "Lister tous les comptes bancaires (sans pagination - utilise pour les listes deroulantes)")
    public List<CompteResponse> findAll() {
        return compteService.findAll();
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('COMPTE_READ')")
    @Operation(summary = "Lister les comptes bancaires (pagine)")
    public Page<CompteResponse> findAllPaged(@PageableDefault(size = 10, sort = "iban") Pageable pageable) {
        return compteService.getAllPaginated(pageable).map(CompteResponse::fromEntity);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('COMPTE_READ')")
    @Operation(summary = "Consulter le detail d'un compte bancaire")
    public CompteResponse findById(@PathVariable Long id) {
        return compteService.findById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('COMPTE_UPDATE')")
    @Operation(summary = "Modifier un compte bancaire")
    public CompteResponse update(@PathVariable Long id, @RequestBody CompteRequest request) {
        return compteService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('COMPTE_DELETE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer un compte bancaire")
    public void delete(@PathVariable Long id) {
        compteService.delete(id);
    }
}
