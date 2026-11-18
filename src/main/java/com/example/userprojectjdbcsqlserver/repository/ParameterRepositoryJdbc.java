package com.example.userprojectjdbcsqlserver.repository;

import com.example.userprojectjdbcsqlserver.entity.ParameterDefinition;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ParameterRepositoryJdbc implements ParameterRepository {

    private static final RowMapper<ParameterDefinition> ROW_MAPPER = (rs, rowNum) -> ParameterDefinition.builder()
            .code(rs.getString("code"))
            .category(rs.getString("category"))
            .valueType(rs.getString("value_type"))
            .defaultValue(rs.getString("default_value"))
            .minValue((Integer) rs.getObject("min_value"))
            .maxValue((Integer) rs.getObject("max_value"))
            .visible(rs.getBoolean("visible"))
            .modifiable(rs.getBoolean("modifiable"))
            .requiresRestart(rs.getBoolean("requires_restart"))
            .label(rs.getString("label"))
            .storedValue(rs.getString("stored_value"))
            .build();

    private static final String SELECT =
            "SELECT d.code, d.category, d.value_type, d.default_value, d.min_value, d.max_value, d.visible, " +
            "d.modifiable, d.requires_restart, d.label, p.value AS stored_value " +
            "FROM dbo.app_parameter_definition d LEFT JOIN dbo.app_parameter p ON p.code = d.code ";

    private final NamedParameterJdbcTemplate jdbc;

    public ParameterRepositoryJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ParameterDefinition> findAllVisible() {
        return jdbc.query(SELECT + "WHERE d.visible = 1 ORDER BY d.category, d.code", ROW_MAPPER);
    }

    @Override
    public Optional<ParameterDefinition> findByCode(String code) {
        return jdbc.query(SELECT + "WHERE d.code = :code", new MapSqlParameterSource("code", code), ROW_MAPPER)
                .stream().findFirst();
    }

    @Override
    public void upsertValue(String code, String value, String updatedBy) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("code", code)
                .addValue("value", value)
                .addValue("updatedBy", updatedBy);
        int updated = jdbc.update("UPDATE dbo.app_parameter SET value = :value, updated_by = :updatedBy, " +
                "updated_at = SYSUTCDATETIME() WHERE code = :code", params);
        if (updated == 0) {
            jdbc.update("INSERT INTO dbo.app_parameter (code, value, updated_by) VALUES (:code, :value, :updatedBy)", params);
        }
    }

    @Override
    public void insertAudit(String code, String oldValue, String newValue, String changedBy) {
        jdbc.update("INSERT INTO dbo.app_parameter_audit (code, old_value, new_value, changed_by) " +
                        "VALUES (:code, :oldValue, :newValue, :changedBy)",
                new MapSqlParameterSource()
                        .addValue("code", code)
                        .addValue("oldValue", oldValue)
                        .addValue("newValue", newValue)
                        .addValue("changedBy", changedBy));
    }
}
