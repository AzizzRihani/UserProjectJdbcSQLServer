package com.example.userprojectjdbcsqlserver.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Applique le parametre interne display.maxRowsPerPage aux listes paginees : une taille de page demandee au-dela du
 * plafond est ramenee au plafond (comme MAX_RS_SIZE du legacy). Le plafond est relu a chaque appel (cache du service
 * de parametres, invalide a la modification) : le changer prend effet sans redemarrage. Spring Data a aussi un
 * plafond (spring.data.web.pageable.max-page-size) mais il est fixe au demarrage : c'est un parametre d'initialisation.
 */
@Component
public class PageSizeLimiter {

    private final ParameterService parameterService;

    public PageSizeLimiter(ParameterService parameterService) {
        this.parameterService = parameterService;
    }

    public Pageable clamp(Pageable pageable) {
        int max = parameterService.getInt(ParameterService.MAX_ROWS_PER_PAGE);
        if (pageable.isUnpaged() || pageable.getPageSize() <= max) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), max, pageable.getSort());
    }
}
