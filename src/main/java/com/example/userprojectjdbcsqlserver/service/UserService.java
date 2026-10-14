package com.example.userprojectjdbcsqlserver.service;

import com.example.userprojectjdbcsqlserver.dto.UserCreateRequest;
import com.example.userprojectjdbcsqlserver.dto.UserResponse;
import com.example.userprojectjdbcsqlserver.entity.Role;
import com.example.userprojectjdbcsqlserver.entity.User;
import com.example.userprojectjdbcsqlserver.exception.EmailAlreadyExistsException;
import com.example.userprojectjdbcsqlserver.exception.RoleNotFoundException;
import com.example.userprojectjdbcsqlserver.exception.SelfManagementForbiddenException;
import com.example.userprojectjdbcsqlserver.exception.UserNotFoundException;
import com.example.userprojectjdbcsqlserver.exception.UserRolesRequiredException;
import com.example.userprojectjdbcsqlserver.repository.RoleRepository;
import com.example.userprojectjdbcsqlserver.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserResponse> findAll(String actorEmail) {
        log.info("Utilisateur '{}' a consulte la liste des utilisateurs", actorEmail);
        return userRepository.findAll().stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    public Page<UserResponse> getAllPaginated(Pageable pageable, String actorEmail) {
        log.info("Utilisateur '{}' a consulte la liste paginee des utilisateurs", actorEmail);
        return userRepository.findAllP(pageable).map(UserResponse::fromEntity);
    }

    public UserResponse findById(Long id, String actorEmail) {
        User user = getOrThrow(id);
        log.info("Utilisateur '{}' a consulte la fiche de l'utilisateur '{}' (id={})", actorEmail, user.getEmail(), id);
        return UserResponse.fromEntity(user);
    }

    public UserResponse me(Long id, String actorEmail) {
        User user = getOrThrow(id);
        log.info("Utilisateur '{}' a consulte son propre profil", actorEmail);
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse create(UserCreateRequest request, String actorEmail) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        // Absent a la creation : role par defaut USER (comme avant). Liste vide : refusee (au moins un role).
        List<Role> roles = request.getRoleIds() == null ? List.of(defaultRole()) : resolveRoles(request.getRoleIds());

        User user = User.builder()
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .surnom(request.getSurnom())
                .email(request.getEmail())
                .age(request.getAge())
                .numTel(request.getNumTel())
                .motDePasse(passwordEncoder.encode(request.getMotDePasse()))
                .roles(new ArrayList<>(roles))
                .build();

        User saved = userRepository.save(user);
        log.info("Utilisateur '{}' a cree un nouvel utilisateur : '{}' (roles {})", actorEmail, saved.getEmail(), roleCodes(saved.getRoles()));
        return UserResponse.fromEntity(saved);
    }

    @Transactional
    public UserResponse update(Long id, UserCreateRequest request, Long currentUserId, String actorEmail) {
        if (id.equals(currentUserId)) {
            throw new SelfManagementForbiddenException();
        }

        User existing = getOrThrow(id);

        if (request.getEmail() != null && userRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        existing.setNom(request.getNom());
        existing.setPrenom(request.getPrenom());
        existing.setSurnom(request.getSurnom());
        existing.setEmail(request.getEmail());
        existing.setAge(request.getAge());
        existing.setNumTel(request.getNumTel());
        String rolesBefore = roleCodes(existing.getRoles());
        // Absent a la modification : roles inchanges. Liste vide : refusee (au moins un role).
        if (request.getRoleIds() != null) {
            existing.setRoles(new ArrayList<>(resolveRoles(request.getRoleIds())));
        }
        if (request.getMotDePasse() != null && !request.getMotDePasse().isBlank()) {
            existing.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
        }

        User updated = userRepository.update(existing);
        log.info("Utilisateur '{}' a modifie l'utilisateur '{}' (id={}, roles {} -> {})",
                actorEmail, updated.getEmail(), id, rolesBefore, roleCodes(updated.getRoles()));
        return UserResponse.fromEntity(updated);
    }

    public void delete(Long id, Long currentUserId, String actorEmail) {
        if (id.equals(currentUserId)) {
            throw new SelfManagementForbiddenException();
        }
        User target = getOrThrow(id);
        userRepository.deleteById(id);
        log.info("Utilisateur '{}' a supprime l'utilisateur '{}' (id={})", actorEmail, target.getEmail(), id);
    }

    private User getOrThrow(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    private Role defaultRole() {
        return roleRepository.findByCode("USER")
                .orElseThrow(() -> new IllegalStateException("Role par defaut 'USER' introuvable en base"));
    }

    // Ids -> roles : elements null ignores, doublons supprimes (ordre conserve), id inconnu -> 404.
    // Liste vide (ou uniquement des null) -> 400 : un utilisateur doit avoir au moins un role.
    private List<Role> resolveRoles(List<Long> roleIds) {
        Set<Long> distinctIds = roleIds.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        if (distinctIds.isEmpty()) {
            throw new UserRolesRequiredException();
        }
        return distinctIds.stream()
                .map(roleId -> roleRepository.findById(roleId).orElseThrow(() -> new RoleNotFoundException(roleId)))
                .toList();
    }

    private String roleCodes(List<Role> roles) {
        return roles.stream().map(Role::getCode).collect(Collectors.joining("+"));
    }
}
