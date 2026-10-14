package com.example.userprojectjdbcsqlserver.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.userprojectjdbcsqlserver.dto.BanqueRequest;
import com.example.userprojectjdbcsqlserver.dto.BanqueResponse;
import com.example.userprojectjdbcsqlserver.entity.Banque;
import com.example.userprojectjdbcsqlserver.exception.BanqueEnUtilisationException;
import com.example.userprojectjdbcsqlserver.exception.BanqueNotFoundException;
import com.example.userprojectjdbcsqlserver.exception.CodeBanqueAlreadyExistsException;
import com.example.userprojectjdbcsqlserver.repository.BanqueRepository;
import com.example.userprojectjdbcsqlserver.repository.CompteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class BanqueService {

    private final BanqueRepository banqueRepository;
    private final CompteRepository compteRepository;

    public BanqueService(BanqueRepository banqueRepository, CompteRepository compteRepository) {
        this.banqueRepository = banqueRepository;
        this.compteRepository = compteRepository;
    }

    public List<BanqueResponse> findAll() {
        return banqueRepository.findAll().stream()
                .map(BanqueResponse::fromEntity)
                .toList();
    }

    public Page<Banque> getAllPaginated(Pageable pageable) {
        return banqueRepository.findAllP(pageable);
    }

    public BanqueResponse findById(Long id) {
        return BanqueResponse.fromEntity(getOrThrow(id));
    }

    public BanqueResponse create(BanqueRequest request) {
        if (request.getCodeBanque() != null && banqueRepository.existsByCodeBanque(request.getCodeBanque())) {
            throw new CodeBanqueAlreadyExistsException(request.getCodeBanque());
        }

        Banque banque = Banque.builder()
                .nom(request.getNom())
                .codeSwift(request.getCodeSwift())
                .codeBanque(request.getCodeBanque())
                .adresse(request.getAdresse())
                .ville(request.getVille())
                .pays(request.getPays())
                .telephone(request.getTelephone())
                .email(request.getEmail())
                .actif(request.getActif() != null ? request.getActif() : Boolean.TRUE)
                .build();

        Banque saved = banqueRepository.save(banque);
        log.info("Banque creee : '{}' (id={})", saved.getNom(), saved.getId());
        return BanqueResponse.fromEntity(saved);
    }

    public BanqueResponse update(Long id, BanqueRequest request) {
        Banque existing = getOrThrow(id);

        if (request.getCodeBanque() != null && banqueRepository.existsByCodeBanqueAndIdNot(request.getCodeBanque(), id)) {
            throw new CodeBanqueAlreadyExistsException(request.getCodeBanque());
        }

        existing.setNom(request.getNom());
        existing.setCodeSwift(request.getCodeSwift());
        existing.setCodeBanque(request.getCodeBanque());
        existing.setAdresse(request.getAdresse());
        existing.setVille(request.getVille());
        existing.setPays(request.getPays());
        existing.setTelephone(request.getTelephone());
        existing.setEmail(request.getEmail());
        if (request.getActif() != null) {
            existing.setActif(request.getActif());
        }

        Banque updated = banqueRepository.update(existing);
        log.info("Banque modifiee : '{}' (id={})", updated.getNom(), id);
        return BanqueResponse.fromEntity(updated);
    }

    public void delete(Long id) {
        Banque target = getOrThrow(id);
        if (compteRepository.existsByBanqueId(id)) {
            throw new BanqueEnUtilisationException(id);
        }
        banqueRepository.deleteById(id);
        log.info("Banque supprimee : '{}' (id={})", target.getNom(), id);
    }

    private Banque getOrThrow(Long id) {
        return banqueRepository.findById(id).orElseThrow(() -> new BanqueNotFoundException(id));
    }
}
