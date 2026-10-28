package com.example.userprojectjdbcsqlserver.controller;

import com.example.userprojectjdbcsqlserver.dto.BanqueRequest;
import com.example.userprojectjdbcsqlserver.dto.BanqueResponse;
import com.example.userprojectjdbcsqlserver.service.BanqueService;
import com.example.userprojectjdbcsqlserver.security.RequiresFunction;
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
@RequestMapping("/banques")
@Tag(name = "Banques")
@RequiresFunction("BANQUES")
public class BanqueController {

    private final BanqueService banqueService;

    public BanqueController(BanqueService banqueService) {
        this.banqueService = banqueService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('BANQUE_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Creer une banque")
    public BanqueResponse create(@RequestBody BanqueRequest request) {
        return banqueService.create(request);
    }

    // Lookup (listes deroulantes / libelles) utilise aussi par les ecrans Comptes : la fonction COMPTES suffit.
    @GetMapping
    @RequiresFunction({"BANQUES", "COMPTES"})
    @PreAuthorize("hasAuthority('BANQUE_READ')")
    @Operation(summary = "Lister toutes les banques (sans pagination - utilise pour les listes deroulantes)")
    public List<BanqueResponse> findAll() {
        return banqueService.findAll();
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('BANQUE_READ')")
    @Operation(summary = "Lister les banques (pagine)")
    public Page<BanqueResponse> findAllPaged(@PageableDefault(size = 10, sort = "nom") Pageable pageable) {
        return banqueService.getAllPaginated(pageable).map(BanqueResponse::fromEntity);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BANQUE_READ')")
    @Operation(summary = "Consulter le detail d'une banque")
    public BanqueResponse findById(@PathVariable Long id) {
        return banqueService.findById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('BANQUE_UPDATE')")
    @Operation(summary = "Modifier une banque")
    public BanqueResponse update(@PathVariable Long id, @RequestBody BanqueRequest request) {
        return banqueService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('BANQUE_DELETE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer une banque")
    public void delete(@PathVariable Long id) {
        banqueService.delete(id);
    }
}
