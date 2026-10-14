package com.example.userprojectjdbcsqlserver.dto;

import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
public class UserUpdateRequest {
    private String nom;
    private String prenom;
    private String surnom;
    private String email;
    private Integer age;
    private String numTel;
    @ToString.Exclude
    private String motDePasse;
    private List<Long> roleIds;
    /** Optionnel : si renseigne, le mot de passe est mis a jour et re-hache. */
}
