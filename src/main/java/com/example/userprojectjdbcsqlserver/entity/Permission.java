package com.example.userprojectjdbcsqlserver.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Permission {
    private Long id;
    private String code;
    private String module;
    private String label;
}
