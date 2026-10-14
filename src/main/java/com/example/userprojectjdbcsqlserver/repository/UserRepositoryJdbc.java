package com.example.userprojectjdbcsqlserver.repository;

import com.example.userprojectjdbcsqlserver.entity.Role;
import com.example.userprojectjdbcsqlserver.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class UserRepositoryJdbc implements UserRepository {

    // Les roles ne sont PAS charges ici : un JOIN un-a-plusieurs dupliquerait les lignes utilisateur dans
    // la fenetre OFFSET/FETCH de findAllP. Ils sont charges par une requete separee (attachRoles).
    private static final RowMapper<User> ROW_MAPPER = (rs, rowNum) -> User.builder()
            .id(rs.getLong("id"))
            .nom(rs.getString("nom"))
            .prenom(rs.getString("prenom"))
            .surnom(rs.getString("surnom"))
            .email(rs.getString("email"))
            .age((Integer) rs.getObject("age"))
            .numTel(rs.getString("num_tel"))
            .motDePasse(rs.getString("mot_de_passe"))
            .build();

    private record UserRole(long userId, Role role) {}

    private static final RowMapper<UserRole> USER_ROLE_MAPPER = (rs, rowNum) -> new UserRole(
            rs.getLong("user_id"),
            Role.builder()
                    .id(rs.getLong("id"))
                    .code(rs.getString("code"))
                    .label(rs.getString("label"))
                    .build());

    private final NamedParameterJdbcTemplate jdbc;

    public UserRepositoryJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SELECT_COLUMNS = "id, nom, prenom, surnom, email, age, num_tel, mot_de_passe";

    // Mappe les proprietes Java (cote Pageable/Sort) vers les vraies colonnes SQL : evite d'injecter
    // un nom de colonne arbitraire venu du client directement dans la clause ORDER BY.
    // mot_de_passe est volontairement absent : aucune raison de trier sur le hash.
    // Pas de tri par role : un utilisateur peut en avoir plusieurs (cle inconnue -> retombe sur id).
    private static final Map<String, String> SORTABLE_COLUMNS = Map.of(
            "id", "id",
            "nom", "nom",
            "prenom", "prenom",
            "surnom", "surnom",
            "email", "email",
            "age", "age",
            "numTel", "num_tel"
    );

    @Override
    public List<User> findAll() {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM dbo.utilisateurs ORDER BY id";
        List<User> users = jdbc.query(sql, ROW_MAPPER);
        // Tous les utilisateurs : pas de "IN (:ids)" (limite de ~2100 parametres de SQL Server).
        attachRoles(users, null);
        return users;
    }

        @Override
        public Page<User> findAllP(Pageable pageable) {
            Long total = jdbc.queryForObject("SELECT COUNT(*) FROM dbo.utilisateurs", new MapSqlParameterSource(), Long.class);

            String sql = "SELECT " + SELECT_COLUMNS + " FROM dbo.utilisateurs " +
                    "ORDER BY " + buildOrderByClause(pageable.getSort()) + " " +
                    "OFFSET :offset ROWS FETCH NEXT :pageSize ROWS ONLY";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("offset", pageable.getOffset())
                    .addValue("pageSize", pageable.getPageSize());
            List<User> content = jdbc.query(sql, params, ROW_MAPPER);
            attachRoles(content, content.stream().map(User::getId).toList());
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

    // Charge les roles des utilisateurs donnes en UNE requete (userIds == null : tous les utilisateurs).
    private void attachRoles(List<User> users, Collection<Long> userIds) {
        if (users.isEmpty()) {
            return;
        }
        String sql = "SELECT ur.user_id, r.id, r.code, r.label FROM dbo.user_roles ur " +
                "JOIN dbo.roles r ON r.id = ur.role_id " +
                (userIds != null ? "WHERE ur.user_id IN (:userIds) " : "") +
                "ORDER BY r.id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (userIds != null) {
            params.addValue("userIds", userIds);
        }
        Map<Long, List<Role>> rolesByUser = new HashMap<>();
        for (UserRole userRole : jdbc.query(sql, params, USER_ROLE_MAPPER)) {
            rolesByUser.computeIfAbsent(userRole.userId(), id -> new ArrayList<>()).add(userRole.role());
        }
        users.forEach(user -> user.setRoles(rolesByUser.getOrDefault(user.getId(), new ArrayList<>())));
    }

    @Override
    public Optional<User> findById(Long id) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM dbo.utilisateurs WHERE id = :id";
        List<User> results = jdbc.query(sql, new MapSqlParameterSource("id", id), ROW_MAPPER);
        attachRoles(results, List.of(id));
        return results.stream().findFirst();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM dbo.utilisateurs WHERE email = :email";
        List<User> results = jdbc.query(sql, new MapSqlParameterSource("email", email), ROW_MAPPER);
        attachRoles(results, results.stream().map(User::getId).toList());
        return results.stream().findFirst();
    }

    @Override
    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM dbo.utilisateurs WHERE email = :email";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("email", email), Integer.class);
        return count != null && count > 0;
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, Long id) {
        String sql = "SELECT COUNT(*) FROM dbo.utilisateurs WHERE email = :email AND id <> :id";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("email", email)
                .addValue("id", id);
        Integer count = jdbc.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    @Override
    public boolean existsByRoleId(Long roleId) {
        String sql = "SELECT COUNT(*) FROM dbo.user_roles WHERE role_id = :roleId";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("roleId", roleId), Integer.class);
        return count != null && count > 0;
    }

    @Override
    public User save(User user) {
        String sql = "INSERT INTO dbo.utilisateurs (nom, prenom, surnom, email, age, num_tel, mot_de_passe) " +
                "VALUES (:nom, :prenom, :surnom, :email, :age, :numTel, :motDePasse)";
        MapSqlParameterSource params = toParams(user);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        user.setId(key != null ? key.longValue() : null);
        syncRoles(user);
        return user;
    }

    @Override
    public User update(User user) {
        String sql = "UPDATE dbo.utilisateurs SET nom = :nom, prenom = :prenom, surnom = :surnom, email = :email, " +
                "age = :age, num_tel = :numTel, mot_de_passe = :motDePasse WHERE id = :id";
        MapSqlParameterSource params = toParams(user).addValue("id", user.getId());
        jdbc.update(sql, params);
        syncRoles(user);
        return user;
    }

    // Differentiel (supprime les roles retires, ajoute les nouveaux) plutot que "tout supprimer puis
    // reinserer" : une requete concurrente (PermissionRefreshFilter) ne voit jamais l'utilisateur sans
    // aucun role pendant la mise a jour. A appeler dans la transaction du service.
    private void syncRoles(User user) {
        Set<Long> wanted = user.getRoles().stream().map(Role::getId).collect(Collectors.toCollection(HashSet::new));
        Set<Long> current = new HashSet<>(jdbc.queryForList(
                "SELECT role_id FROM dbo.user_roles WHERE user_id = :userId",
                new MapSqlParameterSource("userId", user.getId()), Long.class));

        Set<Long> toRemove = new HashSet<>(current);
        toRemove.removeAll(wanted);
        if (!toRemove.isEmpty()) {
            jdbc.update("DELETE FROM dbo.user_roles WHERE user_id = :userId AND role_id IN (:roleIds)",
                    new MapSqlParameterSource().addValue("userId", user.getId()).addValue("roleIds", toRemove));
        }

        Set<Long> toAdd = new HashSet<>(wanted);
        toAdd.removeAll(current);
        if (!toAdd.isEmpty()) {
            SqlParameterSource[] batch = toAdd.stream()
                    .map(roleId -> new MapSqlParameterSource().addValue("userId", user.getId()).addValue("roleId", roleId))
                    .toArray(SqlParameterSource[]::new);
            jdbc.batchUpdate("INSERT INTO dbo.user_roles (user_id, role_id) VALUES (:userId, :roleId)", batch);
        }
    }

    @Override
    public void deleteById(Long id) {
        // user_roles est supprime par ON DELETE CASCADE.
        String sql = "DELETE FROM dbo.utilisateurs WHERE id = :id";
        jdbc.update(sql, new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource toParams(User user) {
        return new MapSqlParameterSource()
                .addValue("nom", user.getNom())
                .addValue("prenom", user.getPrenom())
                .addValue("surnom", user.getSurnom())
                .addValue("email", user.getEmail())
                .addValue("age", user.getAge())
                .addValue("numTel", user.getNumTel())
                .addValue("motDePasse", user.getMotDePasse());
    }
}
