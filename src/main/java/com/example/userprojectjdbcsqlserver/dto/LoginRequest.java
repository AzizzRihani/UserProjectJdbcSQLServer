package com.example.userprojectjdbcsqlserver.dto;

import lombok.Data;
import lombok.ToString;

@Data
public class LoginRequest {
    private String email;
    // Hash Argon2id calcule par le navigateur (64 caracteres hexa) - jamais le mot de passe en clair.
    @ToString.Exclude
    private String motDePasse;
    // Horodatage client en millisecondes (epoch) : la requete est rejetee au-dela de 120 s d'ecart.
    private Long timestamp;
}
