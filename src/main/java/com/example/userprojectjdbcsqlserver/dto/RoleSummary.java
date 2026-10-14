package com.example.userprojectjdbcsqlserver.dto;

import com.example.userprojectjdbcsqlserver.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleSummary {
    private Long id;
    private String code;
    private String label;

    public static RoleSummary fromEntity(Role role) {
        return RoleSummary.builder().id(role.getId()).code(role.getCode()).label(role.getLabel()).build();
    }

    public static List<RoleSummary> fromEntities(List<Role> roles) {
        return roles.stream().map(RoleSummary::fromEntity).toList();
    }
}
