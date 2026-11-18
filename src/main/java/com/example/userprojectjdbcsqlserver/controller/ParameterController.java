package com.example.userprojectjdbcsqlserver.controller;

import com.example.userprojectjdbcsqlserver.dto.ParameterResponse;
import com.example.userprojectjdbcsqlserver.dto.ParameterUpdateRequest;
import com.example.userprojectjdbcsqlserver.dto.ParameterUpdateResponse;
import com.example.userprojectjdbcsqlserver.security.RequiresFunction;
import com.example.userprojectjdbcsqlserver.security.SecurityUser;
import com.example.userprojectjdbcsqlserver.service.ParameterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/parametres")
@Tag(name = "Parametres")
@RequiresFunction("PARAMETRES")
@PreAuthorize("hasAuthority('PARAMETER_MANAGE')")
public class ParameterController {

    private final ParameterService parameterService;

    public ParameterController(ParameterService parameterService) {
        this.parameterService = parameterService;
    }

    @GetMapping
    @Operation(summary = "Lister les parametres internes visibles (valeur effective, defaut, bornes, redemarrage requis)")
    public List<ParameterResponse> findAll() {
        return parameterService.findAll();
    }

    @PutMapping("/{code}")
    @Operation(summary = "Modifier un parametre interne : valide le type et la plage, ecrit l'audit, prend effet sans redemarrage sauf indication contraire (requiresRestart)")
    public ParameterUpdateResponse update(@PathVariable String code, @RequestBody ParameterUpdateRequest request,
                                          @AuthenticationPrincipal SecurityUser currentUser) {
        return parameterService.update(code, request.value(), currentUser.getUsername());
    }
}
