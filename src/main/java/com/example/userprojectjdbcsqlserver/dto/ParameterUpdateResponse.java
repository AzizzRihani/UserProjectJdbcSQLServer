package com.example.userprojectjdbcsqlserver.dto;

// requiresRestart : la valeur est enregistree mais ne sera prise en compte qu'apres un redemarrage (information
// renvoyee a l'administrateur au moment de la modification, pas decouverte plus tard).
public record ParameterUpdateResponse(String code, String value, boolean requiresRestart) {
}
