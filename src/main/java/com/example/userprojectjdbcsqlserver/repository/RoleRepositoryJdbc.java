package com.example.userprojectjdbcsqlserver.repository;

import com.example.userprojectjdbcsqlserver.entity.Role;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class RoleRepositoryJdbc implements RoleRepository {

    private static final RowMapper<Role> ROW_MAPPER = (rs, rowNum) -> Role.builder()
            .id(rs.getLong("id"))
            .code(rs.getString("code"))
            .label(rs.getString("label"))
            .build();

    private final NamedParameterJdbcTemplate jdbc;

    public RoleRepositoryJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SELECT_COLUMNS = "id, code, label";

    @Override
    public List<Role> findAll() {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM dbo.roles ORDER BY id";
        return jdbc.query(sql, ROW_MAPPER);
    }

    @Override
    public Optional<Role> findById(Long id) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM dbo.roles WHERE id = :id";
        List<Role> results = jdbc.query(sql, new MapSqlParameterSource("id", id), ROW_MAPPER);
        return results.stream().findFirst();
    }

    @Override
    public Optional<Role> findByCode(String code) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM dbo.roles WHERE code = :code";
        List<Role> results = jdbc.query(sql, new MapSqlParameterSource("code", code), ROW_MAPPER);
        return results.stream().findFirst();
    }

    @Override
    public boolean existsByCode(String code) {
        String sql = "SELECT COUNT(*) FROM dbo.roles WHERE code = :code";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("code", code), Integer.class);
        return count != null && count > 0;
    }

    @Override
    public Role save(Role role) {
        String sql = "INSERT INTO dbo.roles (code, label) VALUES (:code, :label)";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("code", role.getCode())
                .addValue("label", role.getLabel());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        role.setId(key != null ? key.longValue() : null);
        return role;
    }

    @Override
    public Role update(Role role) {
        String sql = "UPDATE dbo.roles SET label = :label WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("label", role.getLabel())
                .addValue("id", role.getId());
        jdbc.update(sql, params);
        return role;
    }

    @Override
    public void deleteById(Long id) {
        jdbc.update("DELETE FROM dbo.role_permissions WHERE role_id = :id", new MapSqlParameterSource("id", id));
        jdbc.update("DELETE FROM dbo.roles WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    @Override
    public Set<String> findPermissionCodesByRoleId(Long roleId) {
        return findPermissionCodesByRoleIds(List.of(roleId));
    }

    @Override
    public Set<String> findPermissionCodesByRoleIds(Collection<Long> roleIds) {
        // "IN ()" vide est du SQL invalide : un utilisateur sans role n'a simplement aucune permission.
        if (roleIds.isEmpty()) {
            return new HashSet<>();
        }
        String sql = "SELECT DISTINCT p.code FROM dbo.permissions p " +
                "JOIN dbo.role_permissions rp ON rp.permission_id = p.id " +
                "WHERE rp.role_id IN (:roleIds)";
        List<String> codes = jdbc.queryForList(sql, new MapSqlParameterSource("roleIds", roleIds), String.class);
        return new HashSet<>(codes);
    }

    @Override
    public void replacePermissions(Long roleId, Set<String> permissionCodes) {
        jdbc.update("DELETE FROM dbo.role_permissions WHERE role_id = :roleId", new MapSqlParameterSource("roleId", roleId));
        if (permissionCodes.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO dbo.role_permissions (role_id, permission_id) " +
                "SELECT :roleId, id FROM dbo.permissions WHERE code IN (:codes)";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("roleId", roleId)
                .addValue("codes", permissionCodes);
        jdbc.update(sql, params);
    }
}
