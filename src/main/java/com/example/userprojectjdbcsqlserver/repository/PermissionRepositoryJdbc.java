package com.example.userprojectjdbcsqlserver.repository;

import com.example.userprojectjdbcsqlserver.entity.Permission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PermissionRepositoryJdbc implements PermissionRepository {

    private static final RowMapper<Permission> ROW_MAPPER = (rs, rowNum) -> Permission.builder()
            .id(rs.getLong("id"))
            .code(rs.getString("code"))
            .module(rs.getString("module"))
            .label(rs.getString("label"))
            .build();

    private final NamedParameterJdbcTemplate jdbc;

    public PermissionRepositoryJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Permission> findAll() {
        String sql = "SELECT id, code, module, label FROM dbo.permissions ORDER BY module, code";
        return jdbc.query(sql, ROW_MAPPER);
    }
}
