package com.example.userprojectjdbcsqlserver.repository;

import com.example.userprojectjdbcsqlserver.entity.Fonction;
import com.example.userprojectjdbcsqlserver.entity.Profil;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProfilRepository {
    List<Profil> findAll();
    Optional<Profil> findById(Long id);
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
    Profil save(Profil profil);
    Profil update(Profil profil);
    void deleteById(Long id);

    List<Long> findFunctionIdsByProfilId(Long profilId);
    void replaceFunctions(Long profilId, Collection<Long> functionIds);

    // Une seule requete : le profil est valide ET (voit tout OU possede l'une des fonctions demandees).
    // Appelee par l'intercepteur sur chaque ecran protege.
    boolean hasAnyFunction(Long profilId, Collection<String> functionCodes);

    // Fonctions effectives d'un profil (vide si profil invalide ; toutes si "voit tout"), pour le menu du frontend.
    List<Fonction> findEffectiveFunctions(Long profilId);
}
