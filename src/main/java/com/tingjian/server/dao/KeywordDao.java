package com.tingjian.server.dao;

import com.tingjian.server.entity.KeywordEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class KeywordDao {
    private static final String SELECT_COLUMNS = """
            SELECT id, owner_id, phrase, vibration_enabled, priority, enabled, created_at, updated_at
            FROM keyword_rule
            """;

    private final JdbcTemplate jdbc;

    public KeywordDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void create(KeywordEntity entity) {
        jdbc.update("""
                INSERT INTO keyword_rule
                    (id, owner_id, phrase, vibration_enabled, priority, enabled, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, entity.id(), entity.ownerId(), entity.phrase(), entity.vibrationEnabled(),
                entity.priority(), entity.enabled(), entity.createdAt(), entity.updatedAt());
    }

    public Optional<KeywordEntity> find(String id, String ownerId) {
        return jdbc.query(SELECT_COLUMNS + "WHERE id=? AND owner_id=?",
                this::mapRow, id, ownerId).stream().findFirst();
    }

    public List<KeywordEntity> list(String ownerId) {
        return jdbc.query(SELECT_COLUMNS
                        + "WHERE owner_id=? ORDER BY priority DESC, updated_at DESC, id DESC",
                this::mapRow, ownerId);
    }

    public int update(KeywordEntity entity) {
        return jdbc.update("""
                UPDATE keyword_rule
                SET phrase=?, vibration_enabled=?, priority=?, enabled=?, updated_at=?
                WHERE id=? AND owner_id=?
                """, entity.phrase(), entity.vibrationEnabled(), entity.priority(), entity.enabled(),
                entity.updatedAt(), entity.id(), entity.ownerId());
    }

    public int delete(String id, String ownerId) {
        return jdbc.update("DELETE FROM keyword_rule WHERE id=? AND owner_id=?", id, ownerId);
    }

    private KeywordEntity mapRow(ResultSet rs, int row) throws SQLException {
        return new KeywordEntity(
                rs.getString("id"), rs.getString("owner_id"), rs.getString("phrase"),
                rs.getBoolean("vibration_enabled"), rs.getInt("priority"), rs.getBoolean("enabled"),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("updated_at", LocalDateTime.class));
    }
}
