package com.tingjian.server.dao;

import com.tingjian.server.entity.AuthSessionEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class AuthSessionDao {
    private static final String SELECT_COLUMNS = """
            SELECT id, user_id, access_token_hash, refresh_token_hash,
                   access_expires_at, refresh_expires_at, revoked_at, created_at, updated_at
            FROM auth_session
            """;

    private final JdbcTemplate jdbc;

    public AuthSessionDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void create(AuthSessionEntity entity) {
        jdbc.update("""
                INSERT INTO auth_session
                    (id, user_id, access_token_hash, refresh_token_hash, access_expires_at,
                     refresh_expires_at, revoked_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, entity.id(), entity.userId(), entity.accessTokenHash(), entity.refreshTokenHash(),
                entity.accessExpiresAt(), entity.refreshExpiresAt(), entity.revokedAt(),
                entity.createdAt(), entity.updatedAt());
    }

    public Optional<AuthSessionEntity> findActiveByRefreshTokenForUpdate(
            String refreshTokenHash, LocalDateTime now) {
        return jdbc.query(SELECT_COLUMNS + """
                WHERE refresh_token_hash=? AND revoked_at IS NULL AND refresh_expires_at>?
                FOR UPDATE
                """, this::mapRow, refreshTokenHash, now).stream().findFirst();
    }

    public int rotate(AuthSessionEntity entity) {
        return jdbc.update("""
                UPDATE auth_session
                SET access_token_hash=?, refresh_token_hash=?, access_expires_at=?,
                    refresh_expires_at=?, updated_at=?
                WHERE id=? AND revoked_at IS NULL
                """, entity.accessTokenHash(), entity.refreshTokenHash(), entity.accessExpiresAt(),
                entity.refreshExpiresAt(), entity.updatedAt(), entity.id());
    }

    public int revokeByRefreshToken(String refreshTokenHash, LocalDateTime now) {
        return jdbc.update("""
                UPDATE auth_session SET revoked_at=?, updated_at=?
                WHERE refresh_token_hash=? AND revoked_at IS NULL
                """, now, now, refreshTokenHash);
    }

    private AuthSessionEntity mapRow(ResultSet rs, int row) throws SQLException {
        return new AuthSessionEntity(
                rs.getString("id"), rs.getString("user_id"), rs.getString("access_token_hash"),
                rs.getString("refresh_token_hash"),
                rs.getObject("access_expires_at", LocalDateTime.class),
                rs.getObject("refresh_expires_at", LocalDateTime.class),
                rs.getObject("revoked_at", LocalDateTime.class),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("updated_at", LocalDateTime.class));
    }
}
