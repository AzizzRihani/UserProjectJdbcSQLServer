package com.example.userprojectjdbcsqlserver.service;

import com.example.userprojectjdbcsqlserver.config.AppProperties;
import com.example.userprojectjdbcsqlserver.dto.ParameterResponse;
import com.example.userprojectjdbcsqlserver.dto.ParameterUpdateResponse;
import com.example.userprojectjdbcsqlserver.entity.ParameterDefinition;
import com.example.userprojectjdbcsqlserver.exception.ParameterNotFoundException;
import com.example.userprojectjdbcsqlserver.exception.ParameterNotModifiableException;
import com.example.userprojectjdbcsqlserver.exception.ParameterValueInvalidException;
import com.example.userprojectjdbcsqlserver.repository.ParameterRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * PARAMETRES INTERNES : reglages metier stockes en base, modifiables a chaud par un administrateur.
 *
 * Par rapport a PARAMGENERAL du legacy (cache jamais vide, valeurs recopiees dans des champs figes, tout en chaine) :
 *  - aucune copie figee : les consommateurs appellent get/getInt/getBoolean a chaque usage ;
 *  - le cache a une duree de vie (app.parameters.cache-ttl) ET est invalide explicitement a chaque modification,
 *    apres le commit pour qu'une lecture concurrente ne remette pas l'ancienne valeur en cache ;
 *  - chaque parametre est type et borne (ParameterDefinition), la valeur est validee avant ecriture ;
 *  - chaque modification est auditee et indique si un redemarrage est requis.
 * Le cache est propre a cette JVM (comme celui du legacy) : avec plusieurs instances il faudrait diffuser
 * l'invalidation (Spring Cloud Bus, Redis) ou raccourcir le TTL.
 */
@Slf4j
@Service
public class ParameterService {

    public static final String MAX_FAILED_ATTEMPTS = "security.maxFailedAttempts";
    public static final String PASSWORD_MAX_AGE_DAYS = "security.passwordMaxAgeDays";
    public static final String SESSION_TIMEOUT_MINUTES = "security.sessionTimeoutMinutes";
    public static final String MAX_ROWS_PER_PAGE = "display.maxRowsPerPage";
    public static final String ENVIRONMENT_LABEL = "ui.environmentLabel";

    private final ParameterRepository parameterRepository;
    // Optional.empty() est mis en cache aussi : un code sans definition (ex. module.xxx.enabled) ne rappelle pas la base.
    private final Cache<String, Optional<ParameterDefinition>> cache;

    public ParameterService(ParameterRepository parameterRepository, AppProperties appProperties) {
        this.parameterRepository = parameterRepository;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(appProperties.parameters().cacheTtl())
                .maximumSize(1000)
                .build();
    }

    // ---- Lecture (utilisee par le reste de l'application) ----

    public String get(String code) {
        return definition(code).effectiveValue();
    }

    public int getInt(String code) {
        ParameterDefinition definition = definition(code);
        try {
            return Integer.parseInt(definition.effectiveValue());
        } catch (NumberFormatException ex) {
            // Valeur corrompue (modification directe en base) : on retombe sur le defaut plutot que de casser la requete.
            log.warn("Parametre '{}' : valeur stockee illisible '{}', utilisation du defaut '{}'",
                    code, definition.effectiveValue(), definition.getDefaultValue());
            return Integer.parseInt(definition.getDefaultValue());
        }
    }

    public boolean getBoolean(String code) {
        ParameterDefinition definition = definition(code);
        return Boolean.parseBoolean(definition.effectiveValue());
    }

    /**
     * Un module (fonction du menu) est actif sauf si le parametre "module.<code en minuscules>.enabled" existe et vaut
     * false. Un code sans parametre associe n'est donc jamais bloque.
     */
    public boolean isModuleEnabled(String functionCode) {
        String code = "module." + functionCode.toLowerCase(Locale.ROOT) + ".enabled";
        return findDefinition(code).map(definition -> Boolean.parseBoolean(definition.effectiveValue())).orElse(true);
    }

    // ---- Administration ----

    public List<ParameterResponse> findAll() {
        // Lit la base (pas le cache) : l'ecran d'administration montre toujours l'etat reel.
        return ParameterResponse.fromEntities(parameterRepository.findAllVisible());
    }

    @Transactional
    public ParameterUpdateResponse update(String code, String rawValue, String actorEmail) {
        // Base (pas le cache) : on valide et on compare par rapport a la valeur reellement stockee.
        ParameterDefinition definition = parameterRepository.findByCode(code)
                .orElseThrow(() -> new ParameterNotFoundException(code));
        if (!definition.isModifiable()) {
            throw new ParameterNotModifiableException(code);
        }

        String newValue = validate(definition, rawValue);
        String oldValue = definition.effectiveValue();
        if (newValue.equals(oldValue)) {
            return new ParameterUpdateResponse(code, newValue, false);
        }

        parameterRepository.upsertValue(code, newValue, actorEmail);
        parameterRepository.insertAudit(code, oldValue, newValue, actorEmail);
        invalidateAfterCommit(code);
        log.info("Parametre '{}' modifie par '{}' : '{}' -> '{}'{}", code, actorEmail, oldValue, newValue,
                definition.isRequiresRestart() ? " (redemarrage requis)" : "");
        return new ParameterUpdateResponse(code, newValue, definition.isRequiresRestart());
    }

    // ---- Interne ----

    private ParameterDefinition definition(String code) {
        return findDefinition(code).orElseThrow(() -> new ParameterNotFoundException(code));
    }

    private Optional<ParameterDefinition> findDefinition(String code) {
        return cache.get(code, parameterRepository::findByCode);
    }

    // Invalide apres le commit : invalider avant laisserait une lecture concurrente (qui voit encore l'ancienne valeur
    // committee) la remettre en cache jusqu'a la fin du TTL.
    private void invalidateAfterCommit(String code) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    cache.invalidate(code);
                }
            });
        } else {
            cache.invalidate(code);
        }
    }

    private static String validate(ParameterDefinition definition, String rawValue) {
        String code = definition.getCode();
        String value = rawValue == null ? "" : rawValue.trim();
        switch (definition.getValueType()) {
            case ParameterDefinition.TYPE_INT -> {
                int number;
                try {
                    number = Integer.parseInt(value);
                } catch (NumberFormatException ex) {
                    throw ParameterValueInvalidException.notAnInteger(code);
                }
                int min = definition.getMinValue() != null ? definition.getMinValue() : Integer.MIN_VALUE;
                int max = definition.getMaxValue() != null ? definition.getMaxValue() : Integer.MAX_VALUE;
                if (number < min || number > max) {
                    throw ParameterValueInvalidException.outOfRange(code, min, max);
                }
                // Forme canonique ("007" -> "7") : l'audit et la comparaison avec l'ancienne valeur restent coherents.
                return Integer.toString(number);
            }
            case ParameterDefinition.TYPE_BOOLEAN -> {
                String lower = value.toLowerCase(Locale.ROOT);
                if (!lower.equals("true") && !lower.equals("false")) {
                    throw ParameterValueInvalidException.notABoolean(code);
                }
                return lower;
            }
            default -> {
                if (definition.getMaxValue() != null && value.length() > definition.getMaxValue()) {
                    throw ParameterValueInvalidException.tooLong(code, definition.getMaxValue());
                }
                return value;
            }
        }
    }
}
