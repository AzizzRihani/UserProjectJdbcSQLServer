package com.example.userprojectjdbcsqlserver.dto;

import lombok.Data;
import lombok.ToString;

@Data
public class LoginRequest {
    private String email;
    @ToString.Exclude
    private String motDePasse;
}
