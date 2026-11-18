package com.example.userprojectjdbcsqlserver.dto;

// Toujours une chaine, comme dans le JSON : le service la valide selon le type de la definition du parametre.
public record ParameterUpdateRequest(String value) {
}
