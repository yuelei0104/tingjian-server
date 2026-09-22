package com.tingjian.server.service;

import com.tingjian.server.dao.SessionDao;
import com.tingjian.server.dto.SessionMessageResponse;
import com.tingjian.server.dto.SessionResponse;
import com.tingjian.server.entity.ConversationEntity;
import com.tingjian.server.entity.ConversationMessageEntity;
import com.tingjian.server.util.IdGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class SessionService {
    // Temporary local-only owner. Replace with the authenticated user ID before exposing an API.
    private static final String DEV_OWNER = "local-demo";

    private final SessionDao sessionDao;

    public SessionService(SessionDao sessionDao) {
        this.sessionDao = sessionDao;
    }

    public SessionResponse create(String title) {
        String normalizedTitle = title == null || title.isBlank() ? "新会话" : title.strip();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        ConversationEntity session = new ConversationEntity(
                IdGenerator.uuid(), DEV_OWNER, normalizedTitle, "ACTIVE", now, null);
        sessionDao.create(session);
        return toResponse(session);
    }

    public SessionResponse get(String id) {
        return sessionDao.find(id, DEV_OWNER)
                .map(SessionService::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "会话不存在"));
    }

    public List<SessionResponse> list(int page, int size) {
        return sessionDao.list(DEV_OWNER, size, (long) page * size).stream()
                .map(SessionService::toResponse)
                .toList();
    }

    public List<SessionMessageResponse> messages(String sessionId) {
        get(sessionId);
        return sessionDao.messages(sessionId).stream()
                .map(SessionService::toResponse)
                .toList();
    }

    public SessionResponse end(String id) {
        sessionDao.end(id, DEV_OWNER, LocalDateTime.now(ZoneOffset.UTC));
        return get(id);
    }

    @Transactional
    public SessionMessageResponse addMessage(String sessionId, String speaker, String content) {
        String status = sessionDao.lockStatus(sessionId, DEV_OWNER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "会话不存在"));
        if (!"ACTIVE".equals(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "会话已结束");
        }

        ConversationMessageEntity message = new ConversationMessageEntity(
                IdGenerator.uuid(), sessionId, speaker, content, LocalDateTime.now(ZoneOffset.UTC));
        sessionDao.addMessage(message);
        return toResponse(message);
    }

    private static SessionResponse toResponse(ConversationEntity session) {
        return new SessionResponse(
                session.id(), session.title(), session.status(), session.startedAt(), session.endedAt());
    }

    private static SessionMessageResponse toResponse(ConversationMessageEntity message) {
        return new SessionMessageResponse(
                message.id(), message.speaker(), message.content(), message.createdAt());
    }
}
