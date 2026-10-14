package com.example.userprojectjdbcsqlserver.dto;

import com.example.userprojectjdbcsqlserver.entity.Compte;
import com.example.userprojectjdbcsqlserver.entity.TypeCompte;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class CompteResponse {
    private Long id;
    private Long banqueId;
    private String iban;
    private BigDecimal solde;
    private String devise;
    private TypeCompte typeCompte;
    private LocalDate dateOuverture;
    private Boolean actif;

    public static CompteResponse fromEntity(Compte compte) {
        return CompteResponse.builder()
                .id(compte.getId())
                .banqueId(compte.getBanqueId())
                .iban(compte.getIban())
                .solde(compte.getSolde())
                .devise(compte.getDevise())
                .typeCompte(compte.getTypeCompte())
                .dateOuverture(compte.getDateOuverture())
                .actif(compte.getActif())
                .build();
    }
}
