package com.example.userprojectjdbcsqlserver.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Banque {
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
}
