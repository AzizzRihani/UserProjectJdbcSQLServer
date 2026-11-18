package com.example.userprojectjdbcsqlserver.dto;

// Public (avant connexion) : uniquement le libelle d'environnement (DEV, RECETTE, PROD...), rien de sensible.
public record EnvironmentResponse(String environmentLabel) {
}
