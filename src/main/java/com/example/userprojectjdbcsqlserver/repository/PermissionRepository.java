package com.example.userprojectjdbcsqlserver.repository;

import com.example.userprojectjdbcsqlserver.entity.Permission;

import java.util.List;

public interface PermissionRepository {
    List<Permission> findAll();
}
