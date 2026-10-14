package com.example.userprojectjdbcsqlserver.repository;

import com.example.userprojectjdbcsqlserver.entity.Compte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CompteRepository {
    List<Compte> findAll();
    Page<Compte> findAllP(Pageable pageable);
    Optional<Compte> findById(Long id);
    boolean existsByBanqueId(Long banqueId);
    Optional<Compte> findByIban(String iban);
    boolean existsByIban(String iban);
    boolean existsByIbanAndIdNot(String iban, Long id);
    Compte save(Compte compte);
    Compte update(Compte compte);
    void deleteById(Long id);
}
