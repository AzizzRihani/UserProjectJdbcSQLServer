package com.example.userprojectjdbcsqlserver.dto;

import lombok.Data;

import java.util.List;

// Corps atomique de creation/modification d'un profil : le profil et ses fonctions sont enregistres dans la meme
// transaction, donc un profil ne peut pas exister sans ses fonctions (ni l'inverse).
@Data
public class ProfilRequest {
    private String code;
    private String label;
    private Boolean valide;
    private List<Long> functionIds;
}
