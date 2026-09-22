package com.tingjian.server.session;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class SessionRepository {
    private final JdbcTemplate jdbc;

    public SessionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void create(String id, String owner, String title, LocalDateTime now) {
        jdbc.update("INSERT INTO conversation (id, owner_id, title, status, started_at) VALUES (?, ?, ?, 'ACTIVE', ?)",
                id, owner, title, now);
    }

    public Optional<SessionView> find(String id, String owner) {
        return jdbc.query("SELECT id, title, status, started_at, ended_at FROM conversation WHERE id=? AND owner_id=?",
                (rs, row) -> new SessionView(rs.getString("id"), rs.getString("title"),
                        rs.getString("status"), rs.getObject("started_at", LocalDateTime.class),
                        rs.getObject("ended_at", LocalDateTime.class)), id, owner).stream().findFirst();
    }

    public List<SessionView> list(String owner, int size, long offset) {
        return jdbc.query("SELECT id, title, status, started_at, ended_at FROM conversation "
                        + "WHERE owner_id=? ORDER BY started_at DESC, id DESC LIMIT ? OFFSET ?",
                (rs, row) -> new SessionView(rs.getString("id"), rs.getString("title"),
                        rs.getString("status"), rs.getObject("started_at", LocalDateTime.class),
                        rs.getObject("ended_at", LocalDateTime.class)), owner, size, offset);
    }

    public int end(String id, String owner, LocalDateTime now) {
        return jdbc.update("UPDATE conversation SET status='ENDED', ended_at=? "
                + "WHERE id=? AND owner_id=? AND status='ACTIVE'", now, id, owner);
    }

    public Optional<String> lockStatus(String id, String owner) {
        return jdbc.query("SELECT status FROM conversation WHERE id=? AND owner_id=? FOR UPDATE",
                (rs, row) -> rs.getString(1), id, owner).stream().findFirst();
    }

    public void addMessage(String id, String sessionId, String speaker, String content, LocalDateTime now) {
        jdbc.update("INSERT INTO conversation_message (id, conversation_id, speaker, content, created_at) "
                + "VALUES (?, ?, ?, ?, ?)", id, sessionId, speaker, content, now);
    }

    public List<MessageView> messages(String sessionId) {
        return jdbc.query("SELECT id, speaker, content, created_at FROM conversation_message "
                        + "WHERE conversation_id=? ORDER BY created_at, id",
                (rs, row) -> new MessageView(rs.getString("id"), rs.getString("speaker"),
                        rs.getString("content"), rs.getObject("created_at", LocalDateTime.class)), sessionId);
    }

    public record SessionView(String id, String title, String status, LocalDateTime startedAt,
                              LocalDateTime endedAt) {}

    public record MessageView(String id, String speaker, String content, LocalDateTime createdAt) {}
}
