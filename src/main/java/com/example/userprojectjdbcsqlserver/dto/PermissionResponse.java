package com.example.userprojectjdbcsqlserver.dto;

import com.example.userprojectjdbcsqlserver.entity.Permission;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PermissionResponse {
    private Long id;
    private String code;
    private String module;
    private String label;

    public static PermissionResponse fromEntity(Permission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .code(permission.getCode())
                .module(permission.getModule())
                .label(permission.getLabel())
                .build();
    }
}
