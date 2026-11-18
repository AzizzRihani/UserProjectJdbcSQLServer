package com.example.userprojectjdbcsqlserver.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserIdentityRepositoryJdbc implements UserIdentityRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public UserIdentityRepositoryJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Long> findUserId(String provider, String subject) {
        return jdbc.query("SELECT user_id FROM dbo.user_identity WHERE provider = :provider AND subject = :subject",
                        new MapSqlParameterSource().addValue("provider", provider).addValue("subject", subject),
                        (rs, rowNum) -> rs.getLong("user_id"))
                .stream().findFirst();
    }

    @Override
    public boolean existsForUser(Long userId, String provider) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM dbo.user_identity " +
                        "WHERE user_id = :userId AND provider = :provider",
                new MapSqlParameterSource().addValue("userId", userId).addValue("provider", provider), Integer.class);
        return count != null && count > 0;
    }

    @Override
    public void link(Long userId, String provider, String subject, String email) {
        jdbc.update("INSERT INTO dbo.user_identity (user_id, provider, subject, email, last_login_at) " +
                        "VALUES (:userId, :provider, :subject, :email, SYSUTCDATETIME())",
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("provider", provider)
                        .addValue("subject", subject)
                        .addValue("email", email));
    }

    @Override
    public void touchLastLogin(String provider, String subject) {
        jdbc.update("UPDATE dbo.user_identity SET last_login_at = SYSUTCDATETIME() " +
                        "WHERE provider = :provider AND subject = :subject",
                new MapSqlParameterSource().addValue("provider", provider).addValue("subject", subject));
    }
}
