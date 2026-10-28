package com.example.userprojectjdbcsqlserver.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LoginResponse {
    private Long id;
    private String email;
    private String nom;
    private String prenom;
    private List<RoleSummary> roles;
    private List<String> permissions;
    // Fonctions (entrees de menu) du profil, liste plate ; vide = menu vide.
    private List<FonctionResponse> functions;
}
