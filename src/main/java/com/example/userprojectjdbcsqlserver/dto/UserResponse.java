package com.example.userprojectjdbcsqlserver.dto;

import com.example.userprojectjdbcsqlserver.entity.User;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO de sortie : ne contient jamais le mot de passe.
 */
@Data
@Builder
public class UserResponse {
    private Long id;
    private String nom;
    private String prenom;
    private String surnom;
    private String email;
    private Integer age;
    private String numTel;
    private List<RoleSummary> roles;

    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .surnom(user.getSurnom())
                .email(user.getEmail())
                .age(user.getAge())
                .numTel(user.getNumTel())
                .roles(RoleSummary.fromEntities(user.getRoles()))
                .build();
    }
}
