package com.tingjian.server.dao;

import com.tingjian.server.entity.HomeOverviewEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class HomeDao {
    private final JdbcTemplate jdbc;

    public HomeDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public HomeOverviewEntity overview(String ownerId) {
        return jdbc.queryForObject("""
                SELECT
                    (SELECT COUNT(*) FROM conversation c WHERE c.owner_id=?) AS conversation_count,
                    (SELECT COUNT(*) FROM conversation_message cm
                     JOIN conversation c ON c.id=cm.conversation_id
                     WHERE c.owner_id=?) AS message_count,
                    (SELECT COALESCE(SUM(TIMESTAMPDIFF(SECOND, c.started_at, c.ended_at)), 0)
                     FROM conversation c
                     WHERE c.owner_id=? AND c.ended_at IS NOT NULL) AS total_duration_seconds
                """, (rs, row) -> new HomeOverviewEntity(
                rs.getLong("conversation_count"),
                rs.getLong("message_count"),
                rs.getLong("total_duration_seconds")), ownerId, ownerId, ownerId);
    }
}
