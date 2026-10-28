package com.example.userprojectjdbcsqlserver.service;

import com.example.userprojectjdbcsqlserver.dto.ProfilRequest;
import com.example.userprojectjdbcsqlserver.dto.ProfilResponse;
import com.example.userprojectjdbcsqlserver.entity.Fonction;
import com.example.userprojectjdbcsqlserver.entity.Profil;
import com.example.userprojectjdbcsqlserver.exception.FonctionNotFoundException;
import com.example.userprojectjdbcsqlserver.exception.ProfilCodeAlreadyExistsException;
import com.example.userprojectjdbcsqlserver.exception.ProfilEnUtilisationException;
import com.example.userprojectjdbcsqlserver.exception.ProfilNotFoundException;
import com.example.userprojectjdbcsqlserver.exception.ProfilProtegeException;
import com.example.userprojectjdbcsqlserver.repository.FonctionRepository;
import com.example.userprojectjdbcsqlserver.repository.ProfilRepository;
import com.example.userprojectjdbcsqlserver.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProfilService {

    // Garde-fou contre une boucle de parents : l'arbre seede n'a que 2 niveaux, aucune API ne le modifie.
    private static final int MAX_TREE_DEPTH = 10;

    private final ProfilRepository profilRepository;
    private final FonctionRepository fonctionRepository;
    private final UserRepository userRepository;

    public ProfilService(ProfilRepository profilRepository, FonctionRepository fonctionRepository, UserRepository userRepository) {
        this.profilRepository = profilRepository;
        this.fonctionRepository = fonctionRepository;
        this.userRepository = userRepository;
    }

    public List<ProfilResponse> findAll() {
        return profilRepository.findAll().stream().map(this::toResponse).toList();
    }

    public ProfilResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public ProfilResponse create(ProfilRequest request, String actorEmail) {
        String code = requireText(request.getCode(), "code");
        String label = requireText(request.getLabel(), "libelle");
        if (profilRepository.existsByCode(code)) {
            throw new ProfilCodeAlreadyExistsException(code);
        }

        Set<Long> functionIds = normalizeFunctionIds(request.getFunctionIds());
        Profil saved = profilRepository.save(Profil.builder()
                .code(code)
                .label(label)
                .valide(request.getValide() == null || request.getValide())
                .toutVoir(false)
                .build());
        profilRepository.replaceFunctions(saved.getId(), functionIds);
        log.info("Utilisateur '{}' a cree le profil '{}' (id={}, {} fonction(s))", actorEmail, saved.getCode(), saved.getId(), functionIds.size());
        return toResponse(saved);
    }

    @Transactional
    public ProfilResponse update(Long id, ProfilRequest request, String actorEmail) {
        Profil existing = getOrThrow(id);
        requireNotProtected(existing);

        String code = requireText(request.getCode(), "code");
        String label = requireText(request.getLabel(), "libelle");
        if (profilRepository.existsByCodeAndIdNot(code, id)) {
            throw new ProfilCodeAlreadyExistsException(code);
        }

        Set<Long> functionIds = normalizeFunctionIds(request.getFunctionIds());
        existing.setCode(code);
        existing.setLabel(label);
        if (request.getValide() != null) {
            existing.setValide(request.getValide());
        }
        profilRepository.update(existing);
        profilRepository.replaceFunctions(id, functionIds);
        log.info("Utilisateur '{}' a modifie le profil '{}' (id={}, {} fonction(s), valide={})",
                actorEmail, existing.getCode(), id, functionIds.size(), existing.getValide());
        return toResponse(existing);
    }

    @Transactional
    public void delete(Long id, String actorEmail) {
        Profil target = getOrThrow(id);
        requireNotProtected(target);
        if (userRepository.existsByProfilId(id)) {
            throw new ProfilEnUtilisationException(id);
        }
        try {
            profilRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            // Affectation concurrente entre le controle ci-dessus et la suppression : la FK protege.
            throw new ProfilEnUtilisationException(id);
        }
        log.info("Utilisateur '{}' a supprime le profil '{}' (id={})", actorEmail, target.getCode(), id);
    }

    // Ids demandes -> ids a enregistrer : chaque id doit exister, et tous les ANCETRES sont ajoutes (cocher un
    // enfant fait apparaitre son groupe parent dans le menu). Le serveur fait foi, meme si le frontend le fait deja.
    private Set<Long> normalizeFunctionIds(List<Long> requested) {
        Map<Long, Fonction> catalog = fonctionRepository.findAll().stream()
                .collect(Collectors.toMap(Fonction::getId, Function.identity()));
        Set<Long> result = new LinkedHashSet<>();
        if (requested == null) {
            return result;
        }
        for (Long id : requested.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new))) {
            Fonction current = catalog.get(id);
            if (current == null) {
                throw new FonctionNotFoundException(id);
            }
            Set<Long> visited = new HashSet<>();
            int depth = 0;
            while (current != null && visited.add(current.getId()) && depth++ < MAX_TREE_DEPTH) {
                result.add(current.getId());
                current = current.getParentId() != null ? catalog.get(current.getParentId()) : null;
            }
        }
        return result;
    }

    private ProfilResponse toResponse(Profil profil) {
        // Un profil "voit tout" n'a pas de lignes dans profil_fonctions : toutes les fonctions lui sont accordees.
        List<Fonction> catalog = fonctionRepository.findAll();
        List<Long> functionIds = Boolean.TRUE.equals(profil.getToutVoir())
                ? catalog.stream().map(Fonction::getId).toList()
                : profilRepository.findFunctionIdsByProfilId(profil.getId());
        // Les groupes parents sont stockes avec le profil (menu coherent) mais ne sont pas des ecrans : seuls les
        // ecrans (fonctions avec une route) comptent pour l'affichage "nombre de fonctions".
        Set<Long> granted = new HashSet<>(functionIds);
        int screenCount = (int) catalog.stream().filter(f -> f.getRoute() != null && granted.contains(f.getId())).count();
        return ProfilResponse.fromEntity(profil, functionIds, screenCount);
    }

    // SUPER_ADMIN ("profil 0") : ni modification, ni invalidation, ni suppression (garantit qu'un acces total subsiste).
    private static void requireNotProtected(Profil profil) {
        if (Boolean.TRUE.equals(profil.getToutVoir())) {
            throw new ProfilProtegeException();
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("le champ '" + field + "' est obligatoire");
        }
        return value.trim();
    }

    private Profil getOrThrow(Long id) {
        return profilRepository.findById(id).orElseThrow(() -> new ProfilNotFoundException(id));
    }
}
