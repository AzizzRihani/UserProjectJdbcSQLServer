package com.example.userprojectjdbcsqlserver.repository;

import com.example.userprojectjdbcsqlserver.entity.Fonction;
import com.example.userprojectjdbcsqlserver.entity.Profil;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class ProfilRepositoryJdbc implements ProfilRepository {

    private static final RowMapper<Profil> ROW_MAPPER = (rs, rowNum) -> Profil.builder()
            .id(rs.getLong("id"))
            .code(rs.getString("code"))
            .label(rs.getString("label"))
            .valide(rs.getBoolean("valide"))
            .toutVoir(rs.getBoolean("tout_voir"))
            .build();

    private static final String SELECT_COLUMNS = "id, code, label, valide, tout_voir";

    private final NamedParameterJdbcTemplate jdbc;

    public ProfilRepositoryJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Profil> findAll() {
        return jdbc.query("SELECT " + SELECT_COLUMNS + " FROM dbo.profils ORDER BY id", ROW_MAPPER);
    }

    @Override
    public Optional<Profil> findById(Long id) {
        List<Profil> results = jdbc.query("SELECT " + SELECT_COLUMNS + " FROM dbo.profils WHERE id = :id",
                new MapSqlParameterSource("id", id), ROW_MAPPER);
        return results.stream().findFirst();
    }

    @Override
    public boolean existsByCode(String code) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM dbo.profils WHERE code = :code",
                new MapSqlParameterSource("code", code), Integer.class);
        return count != null && count > 0;
    }

    @Override
    public boolean existsByCodeAndIdNot(String code, Long id) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM dbo.profils WHERE code = :code AND id <> :id",
                new MapSqlParameterSource().addValue("code", code).addValue("id", id), Integer.class);
        return count != null && count > 0;
    }

    @Override
    public Profil save(Profil profil) {
        String sql = "INSERT INTO dbo.profils (code, label, valide, tout_voir) VALUES (:code, :label, :valide, :toutVoir)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params(profil), keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        profil.setId(key != null ? key.longValue() : null);
        return profil;
    }

    @Override
    public Profil update(Profil profil) {
        // tout_voir n'est jamais modifie par l'API : seul le seed definit le profil "voit tout".
        jdbc.update("UPDATE dbo.profils SET code = :code, label = :label, valide = :valide WHERE id = :id",
                params(profil).addValue("id", profil.getId()));
        return profil;
    }

    @Override
    public void deleteById(Long id) {
        // profil_fonctions est supprime par ON DELETE CASCADE.
        jdbc.update("DELETE FROM dbo.profils WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    @Override
    public List<Long> findFunctionIdsByProfilId(Long profilId) {
        return jdbc.queryForList("SELECT fonction_id FROM dbo.profil_fonctions WHERE profil_id = :profilId ORDER BY fonction_id",
                new MapSqlParameterSource("profilId", profilId), Long.class);
    }

    @Override
    public void replaceFunctions(Long profilId, Collection<Long> functionIds) {
        jdbc.update("DELETE FROM dbo.profil_fonctions WHERE profil_id = :profilId", new MapSqlParameterSource("profilId", profilId));
        if (functionIds.isEmpty()) {
            return;
        }
        jdbc.update("INSERT INTO dbo.profil_fonctions (profil_id, fonction_id) SELECT :profilId, id FROM dbo.fonctions WHERE id IN (:ids)",
                new MapSqlParameterSource().addValue("profilId", profilId).addValue("ids", functionIds));
    }

    @Override
    public boolean hasAnyFunction(Long profilId, Collection<String> functionCodes) {
        if (profilId == null || functionCodes.isEmpty()) {
            return false;
        }
        String sql = "SELECT CASE WHEN EXISTS (" +
                "SELECT 1 FROM dbo.profils p WHERE p.id = :profilId AND p.valide = 1 AND (p.tout_voir = 1 OR EXISTS (" +
                "SELECT 1 FROM dbo.profil_fonctions pf JOIN dbo.fonctions f ON f.id = pf.fonction_id " +
                "WHERE pf.profil_id = p.id AND f.code IN (:codes)))) THEN 1 ELSE 0 END";
        Integer result = jdbc.queryForObject(sql,
                new MapSqlParameterSource().addValue("profilId", profilId).addValue("codes", functionCodes), Integer.class);
        return result != null && result == 1;
    }

    @Override
    public List<Fonction> findEffectiveFunctions(Long profilId) {
        if (profilId == null) {
            return List.of();
        }
        String sql = "SELECT f.id, f.code, f.label, f.parent_id, par.code AS parent_code, f.route, f.ordre " +
                "FROM dbo.profils pr " +
                "JOIN dbo.fonctions f ON pr.tout_voir = 1 OR EXISTS (" +
                "SELECT 1 FROM dbo.profil_fonctions pf WHERE pf.profil_id = pr.id AND pf.fonction_id = f.id) " +
                "LEFT JOIN dbo.fonctions par ON par.id = f.parent_id " +
                "WHERE pr.id = :profilId AND pr.valide = 1 " +
                "ORDER BY f.ordre, f.id";
        return jdbc.query(sql, new MapSqlParameterSource("profilId", profilId), FonctionRepositoryJdbc.ROW_MAPPER);
    }

    private MapSqlParameterSource params(Profil profil) {
        return new MapSqlParameterSource()
                .addValue("code", profil.getCode())
                .addValue("label", profil.getLabel())
                .addValue("valide", profil.getValide())
                .addValue("toutVoir", profil.getToutVoir());
    }
}
