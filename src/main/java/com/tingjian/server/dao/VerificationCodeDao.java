package com.tingjian.server.dao;

import com.tingjian.server.entity.VerificationCodeEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class VerificationCodeDao {
    private final JdbcTemplate jdbc;

    public VerificationCodeDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void create(VerificationCodeEntity entity) {
        jdbc.update("""
                INSERT INTO auth_verification_code
                    (id, channel, destination, purpose, code_hash, failed_attempts,
                     expires_at, consumed_at, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, entity.id(), entity.channel(), entity.destination(), entity.purpose(),
                entity.codeHash(), entity.failedAttempts(), entity.expiresAt(),
                entity.consumedAt(), entity.createdAt());
    }

    public Optional<VerificationCodeEntity> findForUpdate(String id) {
        return jdbc.query("""
                SELECT id, channel, destination, purpose, code_hash, failed_attempts,
                       expires_at, consumed_at, created_at
                FROM auth_verification_code WHERE id=? FOR UPDATE
                """, this::mapRow, id).stream().findFirst();
    }

    public int consume(String id, LocalDateTime consumedAt) {
        return jdbc.update("""
                UPDATE auth_verification_code SET consumed_at=?
                WHERE id=? AND consumed_at IS NULL
                """, consumedAt, id);
    }

    public void incrementFailedAttempts(String id) {
        jdbc.update("""
                UPDATE auth_verification_code
                SET failed_attempts=failed_attempts + 1 WHERE id=? AND consumed_at IS NULL
                """, id);
    }

    public void deleteExpired(LocalDateTime cutoff) {
        jdbc.update("DELETE FROM auth_verification_code WHERE expires_at < ?", cutoff);
    }

    private VerificationCodeEntity mapRow(ResultSet rs, int row) throws SQLException {
        return new VerificationCodeEntity(
                rs.getString("id"), rs.getString("channel"), rs.getString("destination"),
                rs.getString("purpose"), rs.getString("code_hash"),
                rs.getInt("failed_attempts"),
                rs.getObject("expires_at", LocalDateTime.class),
                rs.getObject("consumed_at", LocalDateTime.class),
                rs.getObject("created_at", LocalDateTime.class));
    }
}
