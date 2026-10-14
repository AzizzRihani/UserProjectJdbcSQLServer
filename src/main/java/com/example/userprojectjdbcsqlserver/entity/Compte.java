package com.example.userprojectjdbcsqlserver.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Compte {
    private Long id;
    private Long banqueId;
    private String iban;
    private BigDecimal solde;
    private String devise;
    private TypeCompte typeCompte;
    private LocalDate dateOuverture;
    private Boolean actif;
}
