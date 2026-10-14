package com.example.userprojectjdbcsqlserver.repository;

import com.example.userprojectjdbcsqlserver.entity.Compte;
import com.example.userprojectjdbcsqlserver.entity.TypeCompte;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class CompteRepositoryJdbc implements CompteRepository {

    private static final RowMapper<Compte> ROW_MAPPER = (rs, rowNum) -> Compte.builder()
            .id(rs.getLong("id"))
            .banqueId(rs.getLong("banque_id"))
            .iban(rs.getString("iban"))
            .solde(rs.getBigDecimal("solde"))
            .devise(rs.getString("devise"))
            .typeCompte(TypeCompte.valueOf(rs.getString("type_compte")))
            .dateOuverture(rs.getObject("date_ouverture", LocalDate.class))
            .actif(rs.getBoolean("actif"))
            .build();

    private final NamedParameterJdbcTemplate jdbc;

    public CompteRepositoryJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SELECT_COLUMNS =
            "id, banque_id, iban, solde, devise, type_compte, date_ouverture, actif";

    // Mappe les proprietes Java (cote Pageable/Sort) vers les vraies colonnes SQL : evite d'injecter
    // un nom de colonne arbitraire venu du client directement dans la clause ORDER BY.
    private static final Map<String, String> SORTABLE_COLUMNS = Map.of(
            "id", "id",
            "banqueId", "banque_id",
            "iban", "iban",
            "solde", "solde",
            "devise", "devise",
            "typeCompte", "type_compte",
            "dateOuverture", "date_ouverture",
            "actif", "actif"
    );

    @Override
    public List<Compte> findAll() {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM dbo.compte ORDER BY id";
        return jdbc.query(sql, ROW_MAPPER);
    }

    @Override
    public Page<Compte> findAllP(Pageable pageable) {
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM dbo.compte", new MapSqlParameterSource(), Long.class);

        String sql = "SELECT " + SELECT_COLUMNS + " FROM dbo.compte " +
                "ORDER BY " + buildOrderByClause(pageable.getSort()) + " " +
                "OFFSET :offset ROWS FETCH NEXT :pageSize ROWS ONLY";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("offset", pageable.getOffset())
                .addValue("pageSize", pageable.getPageSize());
        List<Compte> content = jdbc.query(sql, params, ROW_MAPPER);

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
    public Optional<Compte> findById(Long id) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM dbo.compte WHERE id = :id";
        List<Compte> results = jdbc.query(sql, new MapSqlParameterSource("id", id), ROW_MAPPER);
        return results.stream().findFirst();
    }

    @Override
    public boolean existsByBanqueId(Long banqueId) {
        String sql = "SELECT COUNT(*) FROM dbo.compte WHERE banque_id = :banqueId";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("banqueId", banqueId), Integer.class);
        return count != null && count > 0;
    }

    @Override
    public Optional<Compte> findByIban(String iban) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM dbo.compte WHERE iban = :iban";
        List<Compte> results = jdbc.query(sql, new MapSqlParameterSource("iban", iban), ROW_MAPPER);
        return results.stream().findFirst();
    }

    @Override
    public boolean existsByIban(String iban) {
        String sql = "SELECT COUNT(*) FROM dbo.compte WHERE iban = :iban";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("iban", iban), Integer.class);
        return count != null && count > 0;
    }

    @Override
    public boolean existsByIbanAndIdNot(String iban, Long id) {
        String sql = "SELECT COUNT(*) FROM dbo.compte WHERE iban = :iban AND id <> :id";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("iban", iban)
                .addValue("id", id);
        Integer count = jdbc.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    @Override
    public Compte save(Compte compte) {
        String sql = "INSERT INTO dbo.compte (banque_id, iban, solde, devise, type_compte, date_ouverture, actif) " +
                "VALUES (:banqueId, :iban, :solde, :devise, :typeCompte, :dateOuverture, :actif)";
        MapSqlParameterSource params = toParams(compte);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        compte.setId(key != null ? key.longValue() : null);
        return compte;
    }

    @Override
    public Compte update(Compte compte) {
        String sql = "UPDATE dbo.compte SET banque_id = :banqueId, iban = :iban, " +
                "solde = :solde, devise = :devise, type_compte = :typeCompte, date_ouverture = :dateOuverture, actif = :actif " +
                "WHERE id = :id";
        MapSqlParameterSource params = toParams(compte).addValue("id", compte.getId());
        jdbc.update(sql, params);
        return compte;
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM dbo.compte WHERE id = :id";
        jdbc.update(sql, new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource toParams(Compte compte) {
        return new MapSqlParameterSource()
                .addValue("banqueId", compte.getBanqueId())
                .addValue("iban", compte.getIban())
                .addValue("solde", compte.getSolde())
                .addValue("devise", compte.getDevise())
                .addValue("typeCompte", compte.getTypeCompte().name())
                .addValue("dateOuverture", compte.getDateOuverture())
                .addValue("actif", compte.getActif());
    }
}
