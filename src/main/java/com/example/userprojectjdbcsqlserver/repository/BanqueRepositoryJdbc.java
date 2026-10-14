package com.example.userprojectjdbcsqlserver.repository;

import com.example.userprojectjdbcsqlserver.entity.Banque;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class BanqueRepositoryJdbc implements BanqueRepository {

    private static final RowMapper<Banque> ROW_MAPPER = (rs, rowNum) -> Banque.builder()
            .id(rs.getLong("id"))
            .nom(rs.getString("nom"))
            .codeSwift(rs.getString("code_swift"))
            .codeBanque(rs.getString("code_banque"))
            .adresse(rs.getString("adresse"))
            .ville(rs.getString("ville"))
            .pays(rs.getString("pays"))
            .telephone(rs.getString("telephone"))
            .email(rs.getString("email"))
            .actif(rs.getBoolean("actif"))
            .build();

    private final NamedParameterJdbcTemplate jdbc;

    public BanqueRepositoryJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SELECT_COLUMNS =
            "id, nom, code_swift, code_banque, adresse, ville, pays, telephone, email, actif";

    // Mappe les proprietes Java (cote Pageable/Sort) vers les vraies colonnes SQL : evite d'injecter
    // un nom de colonne arbitraire venu du client directement dans la clause ORDER BY.
    private static final Map<String, String> SORTABLE_COLUMNS = Map.of(
            "id", "id",
            "nom", "nom",
            "codeSwift", "code_swift",
            "codeBanque", "code_banque",
            "adresse", "adresse",
            "ville", "ville",
            "pays", "pays",
            "telephone", "telephone",
            "email", "email",
            "actif", "actif"
    );

    @Override
    public List<Banque> findAll() {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM dbo.banque ORDER BY id";
        return jdbc.query(sql, ROW_MAPPER);
    }

    @Override
    public Page<Banque> findAllP(Pageable pageable) {
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM dbo.banque", new MapSqlParameterSource(), Long.class);

        String sql = "SELECT " + SELECT_COLUMNS + " FROM dbo.banque " +
                "ORDER BY " + buildOrderByClause(pageable.getSort()) + " " +
                "OFFSET :offset ROWS FETCH NEXT :pageSize ROWS ONLY";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("offset", pageable.getOffset())
                .addValue("pageSize", pageable.getPageSize());
        List<Banque> content = jdbc.query(sql, params, ROW_MAPPER);

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    private String buildOrderByClause(Sort sort) {
        if (sort.isUnsorted()) {
            return "id ASC";
        }
        return sort.stream()
                .map(order -> SORTABLE_COLUMNS.getOrDefault(order.getProperty(), "id")
                        + (order.isAscending() ? " ASC" : " DESC"))
                .collect(Collectors.joining(", "));
    }

    @Override
    public Optional<Banque> findById(Long id) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM dbo.banque WHERE id = :id";
        List<Banque> results = jdbc.query(sql, new MapSqlParameterSource("id", id), ROW_MAPPER);
        return results.stream().findFirst();
    }

    @Override
    public Optional<Banque> findByCodeBanque(String codeBanque) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM dbo.banque WHERE code_banque = :codeBanque";
        List<Banque> results = jdbc.query(sql, new MapSqlParameterSource("codeBanque", codeBanque), ROW_MAPPER);
        return results.stream().findFirst();
    }

    @Override
    public boolean existsByCodeBanque(String codeBanque) {
        String sql = "SELECT COUNT(*) FROM dbo.banque WHERE code_banque = :codeBanque";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("codeBanque", codeBanque), Integer.class);
        return count != null && count > 0;
    }

    @Override
    public boolean existsByCodeBanqueAndIdNot(String codeBanque, Long id) {
        String sql = "SELECT COUNT(*) FROM dbo.banque WHERE code_banque = :codeBanque AND id <> :id";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("codeBanque", codeBanque)
                .addValue("id", id);
        Integer count = jdbc.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    @Override
    public Banque save(Banque banque) {
        String sql = "INSERT INTO dbo.banque (nom, code_swift, code_banque, adresse, ville, pays, telephone, email, actif) " +
                "VALUES (:nom, :codeSwift, :codeBanque, :adresse, :ville, :pays, :telephone, :email, :actif)";
        MapSqlParameterSource params = toParams(banque);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        banque.setId(key != null ? key.longValue() : null);
        return banque;
    }

    @Override
    public Banque update(Banque banque) {
        String sql = "UPDATE dbo.banque SET nom = :nom, code_swift = :codeSwift, code_banque = :codeBanque, " +
                "adresse = :adresse, ville = :ville, pays = :pays, telephone = :telephone, email = :email, actif = :actif " +
                "WHERE id = :id";
        MapSqlParameterSource params = toParams(banque).addValue("id", banque.getId());
        jdbc.update(sql, params);
        return banque;
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM dbo.banque WHERE id = :id";
        jdbc.update(sql, new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource toParams(Banque banque) {
        return new MapSqlParameterSource()
                .addValue("nom", banque.getNom())
                .addValue("codeSwift", banque.getCodeSwift())
                .addValue("codeBanque", banque.getCodeBanque())
                .addValue("adresse", banque.getAdresse())
                .addValue("ville", banque.getVille())
                .addValue("pays", banque.getPays())
                .addValue("telephone", banque.getTelephone())
                .addValue("email", banque.getEmail())
                .addValue("actif", banque.getActif());
    }
}
