package com.tingjian.server.dao;

import com.tingjian.server.entity.GlossaryTermEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class GlossaryDao {
    private static final String SELECT_COLUMNS = """
            SELECT id, owner_id, term, alias, language, category, priority, enabled, created_at, updated_at
            FROM glossary_term
            """;

    private final JdbcTemplate jdbc;

    public GlossaryDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void create(GlossaryTermEntity entity) {
        jdbc.update("""
                INSERT INTO glossary_term
                    (id, owner_id, term, alias, language, category, priority, enabled, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, entity.id(), entity.ownerId(), entity.term(), entity.alias(), entity.language(),
                entity.category(), entity.priority(), entity.enabled(), entity.createdAt(), entity.updatedAt());
    }

    public Optional<GlossaryTermEntity> find(String id, String ownerId) {
        return jdbc.query(SELECT_COLUMNS + "WHERE id=? AND owner_id=?",
                this::mapRow, id, ownerId).stream().findFirst();
    }

    public List<GlossaryTermEntity> list(String ownerId) {
        return jdbc.query(SELECT_COLUMNS
                        + "WHERE owner_id=? ORDER BY priority DESC, term ASC, id ASC",
                this::mapRow, ownerId);
    }

    public int update(GlossaryTermEntity entity) {
        return jdbc.update("""
                UPDATE glossary_term
                SET term=?, alias=?, language=?, category=?, priority=?, enabled=?, updated_at=?
                WHERE id=? AND owner_id=?
                """, entity.term(), entity.alias(), entity.language(), entity.category(), entity.priority(),
                entity.enabled(), entity.updatedAt(), entity.id(), entity.ownerId());
    }

    public int delete(String id, String ownerId) {
        return jdbc.update("DELETE FROM glossary_term WHERE id=? AND owner_id=?", id, ownerId);
    }

    private GlossaryTermEntity mapRow(ResultSet rs, int row) throws SQLException {
        return new GlossaryTermEntity(
                rs.getString("id"), rs.getString("owner_id"), rs.getString("term"),
                rs.getString("alias"), rs.getString("language"), rs.getString("category"),
                rs.getInt("priority"), rs.getBoolean("enabled"),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("updated_at", LocalDateTime.class));
    }
}
