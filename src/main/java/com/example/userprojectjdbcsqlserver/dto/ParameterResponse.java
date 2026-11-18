package com.example.userprojectjdbcsqlserver.dto;

import com.example.userprojectjdbcsqlserver.entity.ParameterDefinition;

import java.util.List;

/**
 * Un parametre interne tel qu'affiche par l'ecran d'administration : valeur effective, defaut, bornes et indicateurs.
 * "overridden" : une valeur est stockee en base (sinon c'est le defaut de la definition).
 */
public record ParameterResponse(String code, String category, String valueType, String value, String defaultValue,
                                boolean overridden, Integer minValue, Integer maxValue, boolean modifiable,
                                boolean requiresRestart, String label) {

    public static ParameterResponse fromEntity(ParameterDefinition definition) {
        return new ParameterResponse(definition.getCode(), definition.getCategory(), definition.getValueType(),
                definition.effectiveValue(), definition.getDefaultValue(), definition.getStoredValue() != null,
                definition.getMinValue(), definition.getMaxValue(), definition.isModifiable(),
                definition.isRequiresRestart(), definition.getLabel());
    }

    public static List<ParameterResponse> fromEntities(List<ParameterDefinition> definitions) {
        return definitions.stream().map(ParameterResponse::fromEntity).toList();
    }
}
