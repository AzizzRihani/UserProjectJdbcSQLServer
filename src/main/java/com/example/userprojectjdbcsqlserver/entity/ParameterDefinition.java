package com.example.userprojectjdbcsqlserver.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Definition TYPEE d'un parametre interne (type, defaut, bornes, modifiable, redemarrage requis) + sa valeur
 * stockee eventuelle. Le legacy (PARAMGENERAL) ne porte ni defaut, ni borne, ni indicateur de redemarrage :
 * tout y est une chaine non validee.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParameterDefinition implements Serializable {

    public static final String TYPE_STRING = "STRING";
    public static final String TYPE_INT = "INT";
    public static final String TYPE_BOOLEAN = "BOOLEAN";

    private static final long serialVersionUID = 1L;

    private String code;
    private String category;
    private String valueType;
    private String defaultValue;
    // INT : bornes de la valeur. STRING : maxValue = longueur maximale. BOOLEAN : inutilises.
    private Integer minValue;
    private Integer maxValue;
    private boolean visible;
    private boolean modifiable;
    private boolean requiresRestart;
    private String label;
    // Valeur enregistree dans app_parameter (null = aucune ligne = valeur par defaut).
    private String storedValue;

    public String effectiveValue() {
        return storedValue != null ? storedValue : defaultValue;
    }
}
