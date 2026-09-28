package com.tingjian.server.dao;

import com.tingjian.server.entity.UsageSummaryEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public class UsageDao {
    private final JdbcTemplate jdbc;

    public UsageDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UsageSummaryEntity summary(
            String ownerId, LocalDateTime periodStart, LocalDateTime periodEnd) {
        return jdbc.queryForObject("""
                SELECT
                    (SELECT COUNT(*) FROM conversation c
                     WHERE c.owner_id=? AND c.started_at>=? AND c.started_at<?)
                        AS conversation_count,
                    (SELECT COUNT(*) FROM conversation_message cm
                     JOIN conversation c ON c.id=cm.conversation_id
                     WHERE c.owner_id=? AND cm.created_at>=? AND cm.created_at<?)
                        AS message_count,
                    (SELECT COALESCE(SUM(CHAR_LENGTH(cm.content)), 0)
                     FROM conversation_message cm
                     JOIN conversation c ON c.id=cm.conversation_id
                     WHERE c.owner_id=? AND cm.created_at>=? AND cm.created_at<?)
                        AS text_character_count,
                    (SELECT COALESCE(SUM(TIMESTAMPDIFF(SECOND, c.started_at, c.ended_at)), 0)
                     FROM conversation c
                     WHERE c.owner_id=? AND c.ended_at IS NOT NULL
                       AND c.started_at>=? AND c.started_at<?)
                        AS total_duration_seconds
                """, (rs, row) -> new UsageSummaryEntity(
                rs.getLong("conversation_count"),
                rs.getLong("message_count"),
                rs.getLong("text_character_count"),
                rs.getLong("total_duration_seconds")),
                ownerId, periodStart, periodEnd,
                ownerId, periodStart, periodEnd,
                ownerId, periodStart, periodEnd,
                ownerId, periodStart, periodEnd);
    }
}
