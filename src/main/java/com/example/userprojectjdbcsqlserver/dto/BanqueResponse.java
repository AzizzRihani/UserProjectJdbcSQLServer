package com.example.userprojectjdbcsqlserver.dto;

import com.example.userprojectjdbcsqlserver.entity.Banque;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BanqueResponse {
    private Long id;
    private String nom;
    private String codeSwift;
    private String codeBanque;
    private String adresse;
    private String ville;
    private String pays;
    private String telephone;
    private String email;
    private Boolean actif;

    public static BanqueResponse fromEntity(Banque banque) {
        return BanqueResponse.builder()
                .id(banque.getId())
                .nom(banque.getNom())
                .codeSwift(banque.getCodeSwift())
                .codeBanque(banque.getCodeBanque())
                .adresse(banque.getAdresse())
                .ville(banque.getVille())
                .pays(banque.getPays())
                .telephone(banque.getTelephone())
                .email(banque.getEmail())
                .actif(banque.getActif())
                .build();
    }
}
