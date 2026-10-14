package com.example.userprojectjdbcsqlserver.dto;

import com.example.userprojectjdbcsqlserver.entity.TypeCompte;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CompteRequest {
    private Long banqueId;
    private String iban;
    private BigDecimal solde;
    private String devise;
    private TypeCompte typeCompte;
    private LocalDate dateOuverture;
    private Boolean actif;
}
