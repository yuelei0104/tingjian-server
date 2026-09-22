package com.tingjian.server.dao;

import com.tingjian.server.entity.HistorySummaryEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Repository
public class HistoryDao {
    private static final String SUMMARY_SELECT = """
            SELECT c.id, c.title, c.status, c.started_at, c.ended_at,
                   (SELECT COUNT(*) FROM conversation_message cm
                    WHERE cm.conversation_id=c.id) AS message_count,
                   (SELECT cm.content FROM conversation_message cm
                    WHERE cm.conversation_id=c.id
                    ORDER BY cm.created_at DESC, cm.id DESC LIMIT 1) AS preview
            FROM conversation c
            """;

    private final JdbcTemplate jdbc;

    public HistoryDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<HistorySummaryEntity> search(
            String ownerId, String keyword, int size, long offset) {
        if (keyword == null || keyword.isBlank()) {
            return jdbc.query(SUMMARY_SELECT
                            + "WHERE c.owner_id=? ORDER BY c.started_at DESC, c.id DESC LIMIT ? OFFSET ?",
                    this::mapSummary, ownerId, size, offset);
        }

        String pattern = "%" + keyword.strip().toLowerCase(Locale.ROOT) + "%";
        return jdbc.query(SUMMARY_SELECT
                        + "WHERE c.owner_id=? AND (LOWER(c.title) LIKE ? OR EXISTS ("
                        + "SELECT 1 FROM conversation_message cm "
                        + "WHERE cm.conversation_id=c.id AND LOWER(cm.content) LIKE ?)) "
                        + "ORDER BY c.started_at DESC, c.id DESC LIMIT ? OFFSET ?",
                this::mapSummary, ownerId, pattern, pattern, size, offset);
    }

    public long count(String ownerId, String keyword) {
        Long result;
        if (keyword == null || keyword.isBlank()) {
            result = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM conversation WHERE owner_id=?", Long.class, ownerId);
        } else {
            String pattern = "%" + keyword.strip().toLowerCase(Locale.ROOT) + "%";
            result = jdbc.queryForObject("SELECT COUNT(*) FROM conversation c "
                            + "WHERE c.owner_id=? AND (LOWER(c.title) LIKE ? OR EXISTS ("
                            + "SELECT 1 FROM conversation_message cm "
                            + "WHERE cm.conversation_id=c.id AND LOWER(cm.content) LIKE ?))",
                    Long.class, ownerId, pattern, pattern);
        }
        return result == null ? 0 : result;
    }

    public boolean existsForUpdate(String id, String ownerId) {
        return !jdbc.query(
                "SELECT id FROM conversation WHERE id=? AND owner_id=? FOR UPDATE",
                (rs, row) -> rs.getString(1), id, ownerId).isEmpty();
    }

    public void deleteMessages(String conversationId) {
        jdbc.update("DELETE FROM conversation_message WHERE conversation_id=?", conversationId);
    }

    public int deleteConversation(String id, String ownerId) {
        return jdbc.update("DELETE FROM conversation WHERE id=? AND owner_id=?", id, ownerId);
    }

    private HistorySummaryEntity mapSummary(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new HistorySummaryEntity(
                rs.getString("id"),
                rs.getString("title"),
                rs.getString("status"),
                rs.getObject("started_at", LocalDateTime.class),
                rs.getObject("ended_at", LocalDateTime.class),
                rs.getLong("message_count"),
                rs.getString("preview"));
    }
}
