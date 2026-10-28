package com.example.userprojectjdbcsqlserver.dto;

import lombok.Data;

// profilId null = retirer le profil de l'utilisateur (menu vide).
@Data
public class ProfilAssignRequest {
    private Long profilId;
}
