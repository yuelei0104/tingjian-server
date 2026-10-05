package com.tingjian.server.dao;

import com.tingjian.server.dto.AiSuggestionAction;
import com.tingjian.server.entity.AiSuggestionRequestEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AiSuggestionDao {
    private final JdbcTemplate jdbc;

    public AiSuggestionDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<AiSuggestionRequestEntity> find(String ownerId, String clientRequestId) {
        return jdbc.query("""
                        SELECT owner_id, client_request_id, input_hash, action, language,
                               suggestion, provider, fallback, context_messages,
                               input_characters, output_characters, created_at
                        FROM ai_suggestion_request
                        WHERE owner_id=? AND client_request_id=?
                        """,
                (rs, row) -> new AiSuggestionRequestEntity(
                        rs.getString("owner_id"),
                        rs.getString("client_request_id"),
                        rs.getString("input_hash"),
                        AiSuggestionAction.valueOf(rs.getString("action")),
                        rs.getString("language"),
                        rs.getString("suggestion"),
                        rs.getString("provider"),
                        rs.getBoolean("fallback"),
                        rs.getInt("context_messages"),
                        rs.getInt("input_characters"),
                        rs.getInt("output_characters"),
                        rs.getTimestamp("created_at").toLocalDateTime()),
                ownerId, clientRequestId).stream().findFirst();
    }

    public void insert(AiSuggestionRequestEntity entity) {
        jdbc.update("""
                        INSERT INTO ai_suggestion_request(
                            owner_id, client_request_id, input_hash, action, language,
                            suggestion, provider, fallback, context_messages,
                            input_characters, output_characters, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                entity.ownerId(), entity.clientRequestId(), entity.inputHash(),
                entity.action().name(), entity.language(), entity.suggestion(),
                entity.provider(), entity.fallback(), entity.contextMessages(),
                entity.inputCharacters(), entity.outputCharacters(), entity.createdAt());
    }
}
