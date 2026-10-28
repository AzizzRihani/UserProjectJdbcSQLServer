package com.example.userprojectjdbcsqlserver.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// Profil "But" : ensemble de fonctions (menus) qu'un utilisateur peut voir et ouvrir. Serializable car User
// (qui le reference) est stocke dans la session HTTP.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Profil implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String code;
    private String label;
    // Un profil invalide n'accorde aucune fonction.
    private Boolean valide;
    // "Profil 0" (super-administrateur) : voit toutes les fonctions, sans lignes dans profil_fonctions.
    private Boolean toutVoir;
}
