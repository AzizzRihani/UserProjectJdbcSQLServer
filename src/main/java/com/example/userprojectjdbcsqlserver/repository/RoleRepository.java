package com.example.userprojectjdbcsqlserver.repository;

import com.example.userprojectjdbcsqlserver.entity.Role;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RoleRepository {
    List<Role> findAll();
    Optional<Role> findById(Long id);
    Optional<Role> findByCode(String code);
    boolean existsByCode(String code);
    Role save(Role role);
    Role update(Role role);
    void deleteById(Long id);

    Set<String> findPermissionCodesByRoleId(Long roleId);
    // Union (sans doublon) des permissions de plusieurs roles : un utilisateur peut en avoir plusieurs.
    Set<String> findPermissionCodesByRoleIds(Collection<Long> roleIds);
    void replacePermissions(Long roleId, Set<String> permissionCodes);
}
