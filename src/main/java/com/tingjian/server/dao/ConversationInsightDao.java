package com.tingjian.server.dao;

import com.tingjian.server.entity.ConversationInsightEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ConversationInsightDao {
    private final JdbcTemplate jdbc;

    public ConversationInsightDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<ConversationInsightEntity> find(String conversationId, String ownerId) {
        return jdbc.query("""
                        SELECT conversation_id, owner_id, content_hash, summary_text,
                               highlights_data, action_items_data, keywords_data, tone,
                               generated_by, message_count, updated_at
                        FROM conversation_insight
                        WHERE conversation_id=? AND owner_id=?
                        """,
                (rs, row) -> new ConversationInsightEntity(
                        rs.getString("conversation_id"),
                        rs.getString("owner_id"),
                        rs.getString("content_hash"),
                        rs.getString("summary_text"),
                        readList(rs.getString("highlights_data")),
                        readList(rs.getString("action_items_data")),
                        readList(rs.getString("keywords_data")),
                        rs.getString("tone"),
                        rs.getString("generated_by"),
                        rs.getInt("message_count"),
                        rs.getTimestamp("updated_at").toLocalDateTime()),
                conversationId, ownerId).stream().findFirst();
    }

    public void save(ConversationInsightEntity entity) {
        jdbc.update("""
                        INSERT INTO conversation_insight(
                            conversation_id, owner_id, content_hash, summary_text,
                            highlights_data, action_items_data, keywords_data, tone,
                            generated_by, message_count, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            content_hash=VALUES(content_hash),
                            summary_text=VALUES(summary_text),
                            highlights_data=VALUES(highlights_data),
                            action_items_data=VALUES(action_items_data),
                            keywords_data=VALUES(keywords_data),
                            tone=VALUES(tone),
                            generated_by=VALUES(generated_by),
                            message_count=VALUES(message_count),
                            updated_at=VALUES(updated_at)
                        """,
                entity.conversationId(), entity.ownerId(), entity.contentHash(), entity.summary(),
                writeList(entity.highlights()), writeList(entity.actionItems()),
                writeList(entity.keywords()), entity.tone(), entity.generatedBy(),
                entity.messageCount(), entity.updatedAt());
    }

    private String writeList(List<String> value) {
        return value.stream()
                .map(item -> Base64.getUrlEncoder().withoutPadding().encodeToString(
                        item.getBytes(StandardCharsets.UTF_8)))
                .collect(Collectors.joining("."));
    }

    private List<String> readList(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split("\\."))
                .map(item -> new String(Base64.getUrlDecoder().decode(item), StandardCharsets.UTF_8))
                .toList();
    }
}
