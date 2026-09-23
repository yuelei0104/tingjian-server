package com.tingjian.server.dao;

import com.tingjian.server.entity.QuickPhraseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class QuickPhraseDao {
    private static final String SELECT_COLUMNS = """
            SELECT id, owner_id, content, category, sort_order, enabled, created_at, updated_at
            FROM quick_phrase
            """;

    private final JdbcTemplate jdbc;

    public QuickPhraseDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void create(QuickPhraseEntity entity) {
        jdbc.update("""
                INSERT INTO quick_phrase
                    (id, owner_id, content, category, sort_order, enabled, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, entity.id(), entity.ownerId(), entity.content(), entity.category(), entity.sortOrder(),
                entity.enabled(), entity.createdAt(), entity.updatedAt());
    }

    public Optional<QuickPhraseEntity> find(String id, String ownerId) {
        return jdbc.query(SELECT_COLUMNS + "WHERE id=? AND owner_id=?",
                this::mapRow, id, ownerId).stream().findFirst();
    }

    public List<QuickPhraseEntity> list(String ownerId) {
        return jdbc.query(SELECT_COLUMNS
                        + "WHERE owner_id=? ORDER BY sort_order ASC, updated_at DESC, id ASC",
                this::mapRow, ownerId);
    }

    public int update(QuickPhraseEntity entity) {
        return jdbc.update("""
                UPDATE quick_phrase
                SET content=?, category=?, sort_order=?, enabled=?, updated_at=?
                WHERE id=? AND owner_id=?
                """, entity.content(), entity.category(), entity.sortOrder(), entity.enabled(),
                entity.updatedAt(), entity.id(), entity.ownerId());
    }

    public int delete(String id, String ownerId) {
        return jdbc.update("DELETE FROM quick_phrase WHERE id=? AND owner_id=?", id, ownerId);
    }

    private QuickPhraseEntity mapRow(ResultSet rs, int row) throws SQLException {
        return new QuickPhraseEntity(
                rs.getString("id"), rs.getString("owner_id"), rs.getString("content"),
                rs.getString("category"), rs.getInt("sort_order"), rs.getBoolean("enabled"),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("updated_at", LocalDateTime.class));
    }
}
