package com.example.userprojectjdbcsqlserver.repository;

import com.example.userprojectjdbcsqlserver.entity.ParameterDefinition;

import java.util.List;
import java.util.Optional;

public interface ParameterRepository {
    /** Definitions visibles avec leur valeur stockee (jointure), triees par categorie puis code. */
    List<ParameterDefinition> findAllVisible();

    Optional<ParameterDefinition> findByCode(String code);

    /** Enregistre la valeur (UPDATE puis INSERT si aucune ligne). A appeler dans la transaction du service. */
    void upsertValue(String code, String value, String updatedBy);

    void insertAudit(String code, String oldValue, String newValue, String changedBy);
}
