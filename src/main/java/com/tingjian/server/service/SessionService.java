package com.tingjian.server.service;

import com.tingjian.server.common.BusinessException;
import com.tingjian.server.common.ErrorCode;
import com.tingjian.server.dao.SessionDao;
import com.tingjian.server.dto.SessionMessageResponse;
import com.tingjian.server.dto.SessionResponse;
import com.tingjian.server.entity.ConversationEntity;
import com.tingjian.server.entity.ConversationMessageEntity;
import com.tingjian.server.util.IdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class SessionService {
    private final SessionDao sessionDao;

    public SessionService(SessionDao sessionDao) {
        this.sessionDao = sessionDao;
    }

    public SessionResponse create(String ownerId, String title) {
        String normalizedTitle = title == null || title.isBlank() ? "新会话" : title.strip();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        ConversationEntity session = new ConversationEntity(
                IdGenerator.uuid(), ownerId, normalizedTitle, "ACTIVE", now, null);
        sessionDao.create(session);
        return toResponse(session);
    }

    public SessionResponse get(String ownerId, String id) {
        return sessionDao.find(id, ownerId)
                .map(SessionService::toResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
    }

    public List<SessionResponse> list(String ownerId, int page, int size) {
        return sessionDao.list(ownerId, size, (long) page * size).stream()
                .map(SessionService::toResponse)
                .toList();
    }

    public List<SessionMessageResponse> messages(String ownerId, String sessionId) {
        get(ownerId, sessionId);
        return sessionDao.messages(sessionId).stream()
                .map(SessionService::toResponse)
                .toList();
    }

    public SessionResponse end(String ownerId, String id) {
        sessionDao.end(id, ownerId, LocalDateTime.now(ZoneOffset.UTC));
        return get(ownerId, id);
    }

    @Transactional
    public SessionMessageResponse addMessage(
            String ownerId, String sessionId, String speaker, String content) {
        String status = sessionDao.lockStatus(sessionId, ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
        if (!"ACTIVE".equals(status)) {
            throw new BusinessException(ErrorCode.SESSION_ENDED);
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
