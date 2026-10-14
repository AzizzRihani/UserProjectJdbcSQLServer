package com.example.userprojectjdbcsqlserver.service;

import com.example.userprojectjdbcsqlserver.dto.RoleCreateRequest;
import com.example.userprojectjdbcsqlserver.dto.RolePermissionsRequest;
import com.example.userprojectjdbcsqlserver.dto.RoleResponse;
import com.example.userprojectjdbcsqlserver.dto.RoleUpdateRequest;
import com.example.userprojectjdbcsqlserver.entity.Permission;
import com.example.userprojectjdbcsqlserver.entity.Role;
import com.example.userprojectjdbcsqlserver.exception.RoleCodeAlreadyExistsException;
import com.example.userprojectjdbcsqlserver.exception.RoleEnUtilisationException;
import com.example.userprojectjdbcsqlserver.exception.RoleNotFoundException;
import com.example.userprojectjdbcsqlserver.repository.PermissionRepository;
import com.example.userprojectjdbcsqlserver.repository.RoleRepository;
import com.example.userprojectjdbcsqlserver.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
    }

    public List<RoleResponse> findAll() {
        return roleRepository.findAll().stream()
                .map(role -> RoleResponse.fromEntity(role, sortedPermissionCodes(role.getId())))
                .toList();
    }

    public RoleResponse findById(Long id) {
        Role role = getOrThrow(id);
        return RoleResponse.fromEntity(role, sortedPermissionCodes(id));
    }

    public RoleResponse create(RoleCreateRequest request) {
        if (roleRepository.existsByCode(request.getCode())) {
            throw new RoleCodeAlreadyExistsException(request.getCode());
        }

        Role role = Role.builder()
                .code(request.getCode())
                .label(request.getLabel())
                .build();

        Role saved = roleRepository.save(role);
        log.info("Role cree : '{}' (id={})", saved.getCode(), saved.getId());
        return RoleResponse.fromEntity(saved, List.of());
    }

    public RoleResponse update(Long id, RoleUpdateRequest request) {
        Role existing = getOrThrow(id);
        existing.setLabel(request.getLabel());
        Role updated = roleRepository.update(existing);
        log.info("Role modifie : '{}' (id={})", updated.getCode(), id);
        return RoleResponse.fromEntity(updated, sortedPermissionCodes(id));
    }

    public void delete(Long id) {
        Role target = getOrThrow(id);
        if (userRepository.existsByRoleId(id)) {
            throw new RoleEnUtilisationException(id);
        }
        roleRepository.deleteById(id);
        log.info("Role supprime : '{}' (id={})", target.getCode(), id);
    }

    @Transactional
    public RoleResponse replacePermissions(Long id, RolePermissionsRequest request) {
        Role role = getOrThrow(id);

        Set<String> knownCodes = permissionRepository.findAll().stream()
                .map(Permission::getCode)
                .collect(Collectors.toSet());
        Set<String> requestedCodes = request.getPermissionCodes() != null
                ? new HashSet<>(request.getPermissionCodes())
                : Set.of();
        requestedCodes.retainAll(knownCodes);

        roleRepository.replacePermissions(id, requestedCodes);
        log.info("Permissions du role '{}' (id={}) mises a jour : {}", role.getCode(), id, requestedCodes);
        return RoleResponse.fromEntity(role, sortedPermissionCodes(id));
    }

    private List<String> sortedPermissionCodes(Long roleId) {
        return roleRepository.findPermissionCodesByRoleId(roleId).stream().sorted().toList();
    }

    private Role getOrThrow(Long id) {
        return roleRepository.findById(id).orElseThrow(() -> new RoleNotFoundException(id));
    }
}
