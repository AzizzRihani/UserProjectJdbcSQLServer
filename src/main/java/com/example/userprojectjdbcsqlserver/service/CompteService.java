package com.example.userprojectjdbcsqlserver.service;

import com.example.userprojectjdbcsqlserver.dto.CompteRequest;
import com.example.userprojectjdbcsqlserver.dto.CompteResponse;
import com.example.userprojectjdbcsqlserver.entity.Compte;
import com.example.userprojectjdbcsqlserver.exception.BanqueNotFoundException;
import com.example.userprojectjdbcsqlserver.exception.CompteNotFoundException;
import com.example.userprojectjdbcsqlserver.exception.IbanAlreadyExistsException;
import com.example.userprojectjdbcsqlserver.repository.BanqueRepository;
import com.example.userprojectjdbcsqlserver.repository.CompteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class CompteService {

    private final CompteRepository compteRepository;
    private final BanqueRepository banqueRepository;

    public CompteService(CompteRepository compteRepository, BanqueRepository banqueRepository) {
        this.compteRepository = compteRepository;
        this.banqueRepository = banqueRepository;
    }

    public List<CompteResponse> findAll() {
        return compteRepository.findAll().stream()
                .map(CompteResponse::fromEntity)
                .toList();
    }

    public Page<Compte> getAllPaginated(Pageable pageable) {
        return compteRepository.findAllP(pageable);
    }

    public CompteResponse findById(Long id) {
        return CompteResponse.fromEntity(getOrThrow(id));
    }

    public CompteResponse create(CompteRequest request) {
        if (compteRepository.existsByIban(request.getIban())) {
            throw new IbanAlreadyExistsException(request.getIban());
        }
        if (banqueRepository.findById(request.getBanqueId()).isEmpty()) {
            throw new BanqueNotFoundException(request.getBanqueId());
        }

        Compte compte = Compte.builder()
                .banqueId(request.getBanqueId())
                .iban(request.getIban())
                .solde(request.getSolde() != null ? request.getSolde() : BigDecimal.ZERO)
                .devise(request.getDevise() != null ? request.getDevise() : "EUR")
                .typeCompte(request.getTypeCompte())
                .dateOuverture(request.getDateOuverture() != null ? request.getDateOuverture() : LocalDate.now())
                .actif(request.getActif() != null ? request.getActif() : Boolean.TRUE)
                .build();

        Compte saved = compteRepository.save(compte);
        log.info("Compte cree : iban '{}' (banque id={})", saved.getIban(), saved.getBanqueId());
        return CompteResponse.fromEntity(saved);
    }

    public CompteResponse update(Long id, CompteRequest request) {
        Compte existing = getOrThrow(id);

        if (request.getIban() != null && compteRepository.existsByIbanAndIdNot(request.getIban(), id)) {
            throw new IbanAlreadyExistsException(request.getIban());
        }
        if (request.getBanqueId() != null && banqueRepository.findById(request.getBanqueId()).isEmpty()) {
            throw new BanqueNotFoundException(request.getBanqueId());
        }

        existing.setBanqueId(request.getBanqueId());
        existing.setIban(request.getIban());
        existing.setSolde(request.getSolde());
        existing.setDevise(request.getDevise());
        existing.setTypeCompte(request.getTypeCompte());
        existing.setDateOuverture(request.getDateOuverture());
        if (request.getActif() != null) {
            existing.setActif(request.getActif());
        }

        Compte updated = compteRepository.update(existing);
        log.info("Compte modifie : iban '{}' (id={})", updated.getIban(), id);
        return CompteResponse.fromEntity(updated);
    }

    public void delete(Long id) {
        Compte target = getOrThrow(id);
        compteRepository.deleteById(id);
        log.info("Compte supprime : iban '{}' (id={})", target.getIban(), id);
    }

    private Compte getOrThrow(Long id) {
        return compteRepository.findById(id).orElseThrow(() -> new CompteNotFoundException(id));
    }
}
