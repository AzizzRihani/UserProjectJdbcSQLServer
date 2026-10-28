package com.example.userprojectjdbcsqlserver.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String nom;
    private String prenom;
    private String surnom;
    private String email;
    private Integer age;
    private String numTel;
    private transient String motDePasse;
    @Builder.Default
    private List<Role> roles = new ArrayList<>();
    // Profil (menus) de l'utilisateur : null = aucun profil = menu vide. Charge avec l'utilisateur (LEFT JOIN).
    private Profil profil;
}
