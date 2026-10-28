package com.example.userprojectjdbcsqlserver.dto;

import com.example.userprojectjdbcsqlserver.entity.Profil;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProfilResponse {
    private Long id;
    private String code;
    private String label;
    private Boolean valide;
    private Boolean toutVoir;
    // Fonctions cochees (parents inclus). Vide pour un profil "voit tout" : toutes les fonctions lui sont accordees.
    private List<Long> functionIds;
    // Nombre d'ecrans accordes (fonctions avec route) : les groupes parents ajoutes automatiquement ne comptent pas.
    private Integer screenCount;

    public static ProfilResponse fromEntity(Profil profil, List<Long> functionIds, int screenCount) {
        return ProfilResponse.builder()
                .id(profil.getId())
                .code(profil.getCode())
                .label(profil.getLabel())
                .valide(profil.getValide())
                .toutVoir(profil.getToutVoir())
                .functionIds(functionIds)
                .screenCount(screenCount)
                .build();
    }
}
