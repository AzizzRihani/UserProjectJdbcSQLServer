package com.example.userprojectjdbcsqlserver.dto;

import lombok.Data;

import java.util.List;

@Data
public class RolePermissionsRequest {
    private List<String> permissionCodes;
}
