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

    public List<ConversationEntity> listAll(String ownerId) {
        return jdbc.query("SELECT id, owner_id, title, status, started_at, ended_at "
                        + "FROM conversation WHERE owner_id=? ORDER BY started_at DESC, id DESC",
                (rs, row) -> new ConversationEntity(
                        rs.getString("id"),
                        rs.getString("owner_id"),
                        rs.getString("title"),
                        rs.getString("status"),
                        rs.getObject("started_at", LocalDateTime.class),
                        rs.getObject("ended_at", LocalDateTime.class)),
                ownerId);
    }

    public int end(String id, String ownerId, LocalDateTime endedAt) {
        return jdbc.update("UPDATE conversation SET status='ENDED', ended_at=? "
                + "WHERE id=? AND owner_id=? AND status='ACTIVE'", endedAt, id, ownerId);
    }

    public int rename(String id, String ownerId, String title) {
        return jdbc.update("UPDATE conversation SET title=? WHERE id=? AND owner_id=?",
                title, id, ownerId);
    }

    public Optional<String> lockStatus(String id, String ownerId) {
        return jdbc.query("SELECT status FROM conversation WHERE id=? AND owner_id=? FOR UPDATE",
                (rs, row) -> rs.getString(1), id, ownerId).stream().findFirst();
    }

    public void addMessage(ConversationMessageEntity message) {
        jdbc.update("INSERT INTO conversation_message "
                        + "(id, conversation_id, speaker, content, created_at) VALUES (?, ?, ?, ?, ?)",
                message.id(), message.conversationId(), message.speaker(), message.content(), message.createdAt());
        jdbc.update("INSERT INTO conversation_message_delivery "
                        + "(message_id, conversation_id, client_message_id, sequence_no, created_at) "
                        + "VALUES (?, ?, ?, ?, ?)",
                message.id(), message.conversationId(), message.clientMessageId(),
                message.sequence(), message.createdAt());
    }

    public List<ConversationMessageEntity> messages(String conversationId) {
        return jdbc.query(messageSelect()
                        + "WHERE cm.conversation_id=? ORDER BY delivery.sequence_no",
                this::mapMessage, conversationId);
    }

    public List<ConversationMessageEntity> messagesAfter(
            String conversationId, long afterSequence, int limit) {
        return jdbc.query(messageSelect()
                        + "WHERE cm.conversation_id=? AND delivery.sequence_no>? "
                        + "ORDER BY delivery.sequence_no LIMIT ?",
                this::mapMessage, conversationId, afterSequence, limit);
    }

    public Optional<ConversationMessageEntity> findMessageByClientId(
            String conversationId, String clientMessageId) {
        return jdbc.query(messageSelect()
                        + "WHERE cm.conversation_id=? AND delivery.client_message_id=?",
                this::mapMessage, conversationId, clientMessageId).stream().findFirst();
    }

    public long nextMessageSequence(String conversationId) {
        Long value = jdbc.queryForObject(
                "SELECT COALESCE(MAX(sequence_no), 0) + 1 "
                        + "FROM conversation_message_delivery WHERE conversation_id=?",
                Long.class, conversationId);
        return value == null ? 1L : value;
    }

    private static String messageSelect() {
        return "SELECT cm.id, cm.conversation_id, delivery.client_message_id, "
                + "delivery.sequence_no, cm.speaker, cm.content, cm.created_at "
                + "FROM conversation_message cm JOIN conversation_message_delivery delivery "
                + "ON delivery.message_id=cm.id ";
    }

    private ConversationMessageEntity mapMessage(
            java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new ConversationMessageEntity(
                rs.getString("id"),
                rs.getString("conversation_id"),
                rs.getString("client_message_id"),
                rs.getLong("sequence_no"),
                rs.getString("speaker"),
                rs.getString("content"),
                rs.getObject("created_at", LocalDateTime.class));
    }
}
