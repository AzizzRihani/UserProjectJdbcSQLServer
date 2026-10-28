package com.example.userprojectjdbcsqlserver.repository;

import com.example.userprojectjdbcsqlserver.entity.Fonction;

import java.util.List;

public interface FonctionRepository {
    // Catalogue complet des fonctions (petit, defini par data.sql), ordonne pour construire l'arbre.
    List<Fonction> findAll();
}
