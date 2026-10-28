package com.example.userprojectjdbcsqlserver.repository;

import com.example.userprojectjdbcsqlserver.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    List<User> findAll();
    Page<User> findAllP(Pageable pageable);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByRoleId(Long roleId);
    boolean existsByProfilId(Long profilId);
    void updateProfil(Long userId, Long profilId);
    User save(User user);
    User update(User user);
    void deleteById(Long id);
}
