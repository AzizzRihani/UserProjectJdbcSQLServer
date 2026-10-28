package com.example.userprojectjdbcsqlserver.repository;

import com.example.userprojectjdbcsqlserver.entity.Fonction;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class FonctionRepositoryJdbc implements FonctionRepository {

    static final RowMapper<Fonction> ROW_MAPPER = (rs, rowNum) -> Fonction.builder()
            .id(rs.getLong("id"))
            .code(rs.getString("code"))
            .label(rs.getString("label"))
            .parentId((Long) rs.getObject("parent_id"))
            .parentCode(rs.getString("parent_code"))
            .route(rs.getString("route"))
            .ordre(rs.getInt("ordre"))
            .build();

    private final NamedParameterJdbcTemplate jdbc;

    public FonctionRepositoryJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Fonction> findAll() {
        String sql = "SELECT f.id, f.code, f.label, f.parent_id, par.code AS parent_code, f.route, f.ordre " +
                "FROM dbo.fonctions f LEFT JOIN dbo.fonctions par ON par.id = f.parent_id " +
                "ORDER BY f.ordre, f.id";
        return jdbc.query(sql, ROW_MAPPER);
    }
}
