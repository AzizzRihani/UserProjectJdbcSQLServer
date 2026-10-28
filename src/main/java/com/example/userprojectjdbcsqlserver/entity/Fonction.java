package com.example.userprojectjdbcsqlserver.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Entree de menu. Un groupe (route == null) contient des feuilles (route != null) ; arbre sur 2 niveaux.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Fonction {
    private Long id;
    private String code;
    private String label;
    private Long parentId;
    private String parentCode;
    private String route;
    private Integer ordre;
}
