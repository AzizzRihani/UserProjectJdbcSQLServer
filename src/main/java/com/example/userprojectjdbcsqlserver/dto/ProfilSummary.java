package com.example.userprojectjdbcsqlserver.dto;

import com.example.userprojectjdbcsqlserver.entity.Profil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Profil tel qu'affiche avec un utilisateur (liste, detail, formulaire).
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfilSummary {
    private Long id;
    private String code;
    private String label;
    private Boolean valide;
    private Boolean toutVoir;

    public static ProfilSummary fromEntity(Profil profil) {
        if (profil == null) {
            return null;
        }
        return ProfilSummary.builder()
                .id(profil.getId())
                .code(profil.getCode())
                .label(profil.getLabel())
                .valide(profil.getValide())
                .toutVoir(profil.getToutVoir())
                .build();
    }
}
