package com.tingjian.server.session;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class SessionService {
    // Temporary local-only owner. Replace with the authenticated user ID before exposing an API.
    private static final String DEV_OWNER = "local-demo";

    private final SessionRepository repository;

    public SessionService(SessionRepository repository) {
        this.repository = repository;
    }

    public SessionRepository.SessionView create(String title) {
        String normalized = title == null || title.isBlank() ? "新会话" : title.strip();
        String id = UUID.randomUUID().toString();
        repository.create(id, DEV_OWNER, normalized, LocalDateTime.now(ZoneOffset.UTC));
        return get(id);
    }

    public SessionRepository.SessionView get(String id) {
        return repository.find(id, DEV_OWNER).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "会话不存在"));
    }

    public List<SessionRepository.SessionView> list(int page, int size) {
        return repository.list(DEV_OWNER, size, (long) page * size);
    }

    public List<SessionRepository.MessageView> messages(String sessionId) {
        get(sessionId);
        return repository.messages(sessionId);
    }

    public SessionRepository.SessionView end(String id) {
        repository.end(id, DEV_OWNER, LocalDateTime.now(ZoneOffset.UTC));
        return get(id); // Repeated end requests return the same ended session.
    }

    @Transactional
    public SessionRepository.MessageView addMessage(String sessionId, String speaker, String content) {
        String status = repository.lockStatus(sessionId, DEV_OWNER).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "会话不存在"));
        if (!"ACTIVE".equals(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "会话已结束");
        }
        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        repository.addMessage(id, sessionId, speaker, content, now);
        return new SessionRepository.MessageView(id, speaker, content, now);
    }
}
