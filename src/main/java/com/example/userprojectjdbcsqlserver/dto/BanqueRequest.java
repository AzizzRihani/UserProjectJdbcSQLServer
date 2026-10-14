package com.example.userprojectjdbcsqlserver.dto;

import lombok.Data;

@Data
public class BanqueRequest {
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
