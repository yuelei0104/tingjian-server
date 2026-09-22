package com.tingjian.server.dao;

import com.tingjian.server.entity.ConversationEntity;
import com.tingjian.server.entity.ConversationMessageEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class SessionDao {
    private final JdbcTemplate jdbc;

    public SessionDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void create(ConversationEntity session) {
        jdbc.update("INSERT INTO conversation (id, owner_id, title, status, started_at) "
                        + "VALUES (?, ?, ?, ?, ?)",
                session.id(), session.ownerId(), session.title(), session.status(), session.startedAt());
    }

    public Optional<ConversationEntity> find(String id, String ownerId) {
        return jdbc.query("SELECT id, owner_id, title, status, started_at, ended_at "
                        + "FROM conversation WHERE id=? AND owner_id=?",
                (rs, row) -> new ConversationEntity(
                        rs.getString("id"),
                        rs.getString("owner_id"),
                        rs.getString("title"),
                        rs.getString("status"),
                        rs.getObject("started_at", LocalDateTime.class),
                        rs.getObject("ended_at", LocalDateTime.class)),
                id, ownerId).stream().findFirst();
    }

    public List<ConversationEntity> list(String ownerId, int size, long offset) {
        return jdbc.query("SELECT id, owner_id, title, status, started_at, ended_at FROM conversation "
                        + "WHERE owner_id=? ORDER BY started_at DESC, id DESC LIMIT ? OFFSET ?",
                (rs, row) -> new ConversationEntity(
                        rs.getString("id"),
                        rs.getString("owner_id"),
                        rs.getString("title"),
                        rs.getString("status"),
                        rs.getObject("started_at", LocalDateTime.class),
                        rs.getObject("ended_at", LocalDateTime.class)),
                ownerId, size, offset);
    }

    public int end(String id, String ownerId, LocalDateTime endedAt) {
        return jdbc.update("UPDATE conversation SET status='ENDED', ended_at=? "
                + "WHERE id=? AND owner_id=? AND status='ACTIVE'", endedAt, id, ownerId);
    }

    public Optional<String> lockStatus(String id, String ownerId) {
        return jdbc.query("SELECT status FROM conversation WHERE id=? AND owner_id=? FOR UPDATE",
                (rs, row) -> rs.getString(1), id, ownerId).stream().findFirst();
    }

    public void addMessage(ConversationMessageEntity message) {
        jdbc.update("INSERT INTO conversation_message "
                        + "(id, conversation_id, speaker, content, created_at) VALUES (?, ?, ?, ?, ?)",
                message.id(), message.conversationId(), message.speaker(), message.content(), message.createdAt());
    }

    public List<ConversationMessageEntity> messages(String conversationId) {
        return jdbc.query("SELECT id, conversation_id, speaker, content, created_at "
                        + "FROM conversation_message WHERE conversation_id=? ORDER BY created_at, id",
                (rs, row) -> new ConversationMessageEntity(
                        rs.getString("id"),
                        rs.getString("conversation_id"),
                        rs.getString("speaker"),
                        rs.getString("content"),
                        rs.getObject("created_at", LocalDateTime.class)),
                conversationId);
    }
}
