package com.example.userprojectjdbcsqlserver.dto;

// Public (avant connexion) : fournisseurs SSO actifs, pour n'afficher que les boutons utilisables. Rien de sensible.
public record SsoProviderResponse(String id, String label) {
}
