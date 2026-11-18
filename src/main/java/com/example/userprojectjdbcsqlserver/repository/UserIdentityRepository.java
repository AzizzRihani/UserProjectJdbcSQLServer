package com.example.userprojectjdbcsqlserver.repository;

import java.util.Optional;

/** Liens entre un utilisateur local et son identite chez un fournisseur SSO (table user_identity). */
public interface UserIdentityRepository {

    /** Utilisateur lie a cette identite, s'il existe (identifiant stable chez le fournisseur : "sub" ou "oid"). */
    Optional<Long> findUserId(String provider, String subject);

    /** Vrai si l'utilisateur est deja lie a une identite (quelconque) de ce fournisseur. */
    boolean existsForUser(Long userId, String provider);

    /** Lie l'identite a l'utilisateur (1re connexion SSO). (provider, subject) est unique. */
    void link(Long userId, String provider, String subject, String email);

    void touchLastLogin(String provider, String subject);
}
