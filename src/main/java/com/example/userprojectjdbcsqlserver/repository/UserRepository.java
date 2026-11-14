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

    /**
     * Enregistre un echec de connexion en UN SEUL UPDATE atomique (incrementation + verrouillage au seuil).
     * @return true si le compte est verrouille a l'issue de cet echec ; false si le seuil n'est pas atteint, si le
     *         compte est deja verrouille ou si l'email est inconnu.
     */
    boolean recordFailedLogin(String email, int maxAttempts);

    void resetFailedAttempts(Long userId);

    void unlock(Long userId);
    User save(User user);
    User update(User user);
    void deleteById(Long id);
}
