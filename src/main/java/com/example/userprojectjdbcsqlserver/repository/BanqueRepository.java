package com.example.userprojectjdbcsqlserver.repository;

import com.example.userprojectjdbcsqlserver.entity.Banque;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface BanqueRepository {
    List<Banque> findAll();
    Page<Banque> findAllP(Pageable pageable);

    Optional<Banque> findById(Long id);
    Optional<Banque> findByCodeBanque(String codeBanque);
    boolean existsByCodeBanque(String codeBanque);
    boolean existsByCodeBanqueAndIdNot(String codeBanque, Long id);
    Banque save(Banque banque);
    Banque update(Banque banque);
    void deleteById(Long id);
}
