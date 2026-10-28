package com.example.userprojectjdbcsqlserver.dto;

import com.example.userprojectjdbcsqlserver.entity.Fonction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// Liste plate : le frontend reconstruit l'arbre a partir de parentCode.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FonctionResponse {
    private Long id;
    private String code;
    private String label;
    private String parentCode;
    private String route;
    private Integer ordre;

    public static FonctionResponse fromEntity(Fonction fonction) {
        return FonctionResponse.builder()
                .id(fonction.getId())
                .code(fonction.getCode())
                .label(fonction.getLabel())
                .parentCode(fonction.getParentCode())
                .route(fonction.getRoute())
                .ordre(fonction.getOrdre())
                .build();
    }

    public static List<FonctionResponse> fromEntities(List<Fonction> fonctions) {
        return fonctions.stream().map(FonctionResponse::fromEntity).toList();
    }
}
