package com.example.userprojectjdbcsqlserver.service;

import com.example.userprojectjdbcsqlserver.config.AppProperties;
import com.example.userprojectjdbcsqlserver.dto.ParameterUpdateResponse;
import com.example.userprojectjdbcsqlserver.entity.ParameterDefinition;
import com.example.userprojectjdbcsqlserver.exception.ParameterNotFoundException;
import com.example.userprojectjdbcsqlserver.exception.ParameterNotModifiableException;
import com.example.userprojectjdbcsqlserver.exception.ParameterValueInvalidException;
import com.example.userprojectjdbcsqlserver.repository.ParameterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste la logique du service de parametres internes SANS base de donnees : le service ne depend que de l'INTERFACE
 * ParameterRepository, remplacee ici par une implementation en memoire (c'est l'interet de programmer vers une interface).
 */
class ParameterServiceTest {

    /** Repository en memoire : definitions + valeurs stockees + piste d'audit, avec un compteur de lectures. */
    static class InMemoryParameterRepository implements ParameterRepository {
        final Map<String, ParameterDefinition> definitions = new LinkedHashMap<>();
        final List<String> audit = new ArrayList<>();
        int findByCodeCalls;

        @Override
        public List<ParameterDefinition> findAllVisible() {
            return definitions.values().stream().filter(ParameterDefinition::isVisible)
                    .sorted(Comparator.comparing(ParameterDefinition::getCode)).toList();
        }

        @Override
        public Optional<ParameterDefinition> findByCode(String code) {
            findByCodeCalls++;
            ParameterDefinition stored = definitions.get(code);
            // Copie : le service ne doit jamais modifier l'etat du repository autrement que par upsertValue.
            return Optional.ofNullable(stored).map(d -> ParameterDefinition.builder()
                    .code(d.getCode()).category(d.getCategory()).valueType(d.getValueType())
                    .defaultValue(d.getDefaultValue()).minValue(d.getMinValue()).maxValue(d.getMaxValue())
                    .visible(d.isVisible()).modifiable(d.isModifiable()).requiresRestart(d.isRequiresRestart())
                    .label(d.getLabel()).storedValue(d.getStoredValue()).build());
        }

        @Override
        public void upsertValue(String code, String value, String updatedBy) {
            definitions.get(code).setStoredValue(value);
        }

        @Override
        public void insertAudit(String code, String oldValue, String newValue, String changedBy) {
            audit.add(code + "|" + oldValue + "|" + newValue + "|" + changedBy);
        }

        void define(String code, String type, String defaultValue, Integer min, Integer max, boolean modifiable, boolean restart) {
            definitions.put(code, ParameterDefinition.builder().code(code).category("test").valueType(type)
                    .defaultValue(defaultValue).minValue(min).maxValue(max).visible(true).modifiable(modifiable)
                    .requiresRestart(restart).label(code).build());
        }
    }

    private InMemoryParameterRepository repository;
    private ParameterService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryParameterRepository();
        repository.define("display.maxRowsPerPage", "INT", "100", 10, 1000, true, false);
        repository.define("ui.environmentLabel", "STRING", "DEV", null, 20, true, false);
        repository.define("mail.disabled", "BOOLEAN", "true", null, null, true, true);
        repository.define("security.locked.param", "INT", "3", 1, 10, false, false);
        repository.define("module.comptes.enabled", "BOOLEAN", "true", null, null, true, false);
        AppProperties properties = new AppProperties(new AppProperties.Session(Duration.ofDays(3)),
                new AppProperties.Cors(List.of("https://localhost:4200")), new AppProperties.Parameters(Duration.ofMinutes(5)),
                new AppProperties.Sso(false, "https://localhost:4200", new AppProperties.Sso.Google(null, null),
                        new AppProperties.Sso.Microsoft(null, null, null, "https://login.microsoftonline.com")));
        service = new ParameterService(repository, properties);
    }

    @Test
    void readsTheDefaultWhenNoValueIsStored() {
        assertEquals(100, service.getInt("display.maxRowsPerPage"));
        assertEquals("DEV", service.get("ui.environmentLabel"));
        assertTrue(service.getBoolean("mail.disabled"));
    }

    @Test
    void aModificationIsVisibleImmediatelyWithoutRestart() {
        assertEquals(100, service.getInt("display.maxRowsPerPage")); // met la valeur en cache

        ParameterUpdateResponse result = service.update("display.maxRowsPerPage", "20", "admin@example.com");

        assertEquals("20", result.value());
        assertFalse(result.requiresRestart());
        assertEquals(20, service.getInt("display.maxRowsPerPage"));
    }

    @Test
    void cacheServesTheValueUntilInvalidated() {
        assertEquals(100, service.getInt("display.maxRowsPerPage"));
        int callsAfterFirstRead = repository.findByCodeCalls;
        assertEquals(100, service.getInt("display.maxRowsPerPage"));
        assertEquals(callsAfterFirstRead, repository.findByCodeCalls, "la 2e lecture doit venir du cache");

        // Modification directe en base (hors service) : invisible tant que le cache n'a pas expire ni ete invalide.
        repository.definitions.get("display.maxRowsPerPage").setStoredValue("50");
        assertEquals(100, service.getInt("display.maxRowsPerPage"));

        // Une modification par le service invalide l'entree : la valeur reelle est relue.
        service.update("display.maxRowsPerPage", "60", "admin@example.com");
        assertEquals(60, service.getInt("display.maxRowsPerPage"));
    }

    @Test
    void anUpdateWritesOneAuditLineWithOldAndNewValue() {
        service.update("display.maxRowsPerPage", "20", "admin@example.com");

        assertEquals(List.of("display.maxRowsPerPage|100|20|admin@example.com"), repository.audit);
    }

    @Test
    void anUnchangedValueWritesNothing() {
        service.update("display.maxRowsPerPage", "100", "admin@example.com");

        assertTrue(repository.audit.isEmpty());
        assertEquals(null, repository.definitions.get("display.maxRowsPerPage").getStoredValue());
    }

    @Test
    void aValueOutsideTheRangeIsRefusedAndNothingIsWritten() {
        ParameterValueInvalidException tooBig = assertThrows(ParameterValueInvalidException.class,
                () -> service.update("display.maxRowsPerPage", "5000", "admin@example.com"));
        assertEquals("error.parameter.value.range", tooBig.getMessageKey());

        assertThrows(ParameterValueInvalidException.class,
                () -> service.update("display.maxRowsPerPage", "5", "admin@example.com"));
        assertTrue(repository.audit.isEmpty());
        assertEquals(100, service.getInt("display.maxRowsPerPage"));
    }

    @Test
    void theTypeIsValidated() {
        assertEquals("error.parameter.value.int", assertThrows(ParameterValueInvalidException.class,
                () -> service.update("display.maxRowsPerPage", "abc", "a")).getMessageKey());
        assertEquals("error.parameter.value.boolean", assertThrows(ParameterValueInvalidException.class,
                () -> service.update("mail.disabled", "peut-etre", "a")).getMessageKey());
        assertEquals("error.parameter.value.length", assertThrows(ParameterValueInvalidException.class,
                () -> service.update("ui.environmentLabel", "x".repeat(21), "a")).getMessageKey());
    }

    @Test
    void valuesAreStoredInCanonicalForm() {
        service.update("display.maxRowsPerPage", " 0050 ", "a");
        service.update("mail.disabled", "FALSE", "a");

        assertEquals("50", repository.definitions.get("display.maxRowsPerPage").getStoredValue());
        assertEquals("false", repository.definitions.get("mail.disabled").getStoredValue());
    }

    @Test
    void aNonModifiableParameterIsRefused() {
        assertThrows(ParameterNotModifiableException.class, () -> service.update("security.locked.param", "5", "a"));
        assertTrue(repository.audit.isEmpty());
    }

    @Test
    void anUnknownParameterIsRefused() {
        assertThrows(ParameterNotFoundException.class, () -> service.get("nope"));
        assertThrows(ParameterNotFoundException.class, () -> service.update("nope", "1", "a"));
    }

    @Test
    void aParameterThatNeedsARestartSaysSoAtModificationTime() {
        ParameterUpdateResponse result = service.update("mail.disabled", "false", "admin@example.com");

        assertTrue(result.requiresRestart());
    }

    @Test
    void aCorruptStoredValueFallsBackToTheDefault() {
        repository.definitions.get("display.maxRowsPerPage").setStoredValue("pas-un-nombre");

        assertEquals(100, service.getInt("display.maxRowsPerPage"));
    }

    @Test
    void aModuleIsEnabledUnlessItsParameterSaysFalse() {
        assertTrue(service.isModuleEnabled("COMPTES"));
        assertTrue(service.isModuleEnabled("BANQUES"), "sans parametre associe, un module n'est jamais bloque");

        service.update("module.comptes.enabled", "false", "admin@example.com");

        assertFalse(service.isModuleEnabled("COMPTES"));
        assertTrue(service.isModuleEnabled("BANQUES"));
    }
}
