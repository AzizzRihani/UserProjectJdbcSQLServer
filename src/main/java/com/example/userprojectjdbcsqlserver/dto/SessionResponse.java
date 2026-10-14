package com.example.userprojectjdbcsqlserver.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class SessionResponse {
    private String id;
    private Instant dateCreation;
    private Instant dateExpiration;
    private String userAgent;
    private String adresseIp;
}
