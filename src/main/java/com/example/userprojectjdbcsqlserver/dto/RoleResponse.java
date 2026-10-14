package com.example.userprojectjdbcsqlserver.dto;

import com.example.userprojectjdbcsqlserver.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RoleResponse {
    private Long id;
    private String code;
    private String label;
    private List<String> permissionCodes;

    public static RoleResponse fromEntity(Role role, List<String> permissionCodes) {
        return RoleResponse.builder()
                .id(role.getId())
                .code(role.getCode())
                .label(role.getLabel())
                .permissionCodes(permissionCodes)
                .build();
    }
}
