package com.tingjian.server.service;

import com.tingjian.server.common.BusinessException;
import com.tingjian.server.common.ErrorCode;
import com.tingjian.server.config.RealtimeHandshakeInterceptor;
import com.tingjian.server.dto.RealtimeMessageEvent;
import com.tingjian.server.dto.RealtimeMessageRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RealtimeWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final SessionService sessionService;
    private final ConcurrentHashMap<String, Set<WebSocketSession>> sessions =
            new ConcurrentHashMap<>();

    public RealtimeWebSocketHandler(
            ObjectMapper objectMapper,
            SessionService sessionService) {
        this.objectMapper = objectMapper;
        this.sessionService = sessionService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = requireUserId(session);
        sessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet())
                .add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession socket, TextMessage payload) {
        RealtimeMessageRequest request = null;
        try {
            String userId = requireUserId(socket);
            request = objectMapper.readValue(
                    payload.getPayload(), RealtimeMessageRequest.class);
            validate(request);
            var message = sessionService.addMessage(
                    userId, request.sessionId(), request.clientMessageId(),
                    request.speaker().name(), request.content().strip());
            send(socket, RealtimeMessageEvent.acknowledged(request.sessionId(), message));
            broadcastExcept(userId, socket,
                    RealtimeMessageEvent.message(request.sessionId(), message));
        } catch (BusinessException exception) {
            send(socket, RealtimeMessageEvent.error(
                    request == null ? null : request.sessionId(),
                    request == null ? null : request.clientMessageId(),
                    exception.errorCode().name(), exception.getMessage()));
        } catch (Exception exception) {
            send(socket, RealtimeMessageEvent.error(
                    request == null ? null : request.sessionId(),
                    request == null ? null : request.clientMessageId(),
                    ErrorCode.VALIDATION_ERROR.name(), "实时消息格式不正确"));
        }
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session, CloseStatus status) {
        Object userId = session.getAttributes().get(
                RealtimeHandshakeInterceptor.USER_ID_ATTRIBUTE);
        if (userId instanceof String value) {
            Set<WebSocketSession> userSessions = sessions.get(value);
            if (userSessions != null) {
                userSessions.remove(session);
                if (userSessions.isEmpty()) sessions.remove(value, userSessions);
            }
        }
    }

    private void broadcastExcept(
            String userId, WebSocketSession source, RealtimeMessageEvent event) {
        for (WebSocketSession target : sessions.getOrDefault(userId, Set.of())) {
            if (target != source) send(target, event);
        }
    }

    private void send(WebSocketSession session, RealtimeMessageEvent event) {
        if (!session.isOpen()) return;
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(event)));
            }
        } catch (Exception ignored) {
            // Reconnect and REST recovery handle a dropped acknowledgement.
        }
    }

    private static String requireUserId(WebSocketSession session) {
        Object value = session.getAttributes().get(
                RealtimeHandshakeInterceptor.USER_ID_ATTRIBUTE);
        if (value instanceof String userId && !userId.isBlank()) return userId;
        throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
    }

    private static void validate(RealtimeMessageRequest request) {
        if (request == null || !"MESSAGE".equals(request.type())
                || request.sessionId() == null || request.sessionId().isBlank()
                || request.clientMessageId() == null || request.clientMessageId().isBlank()
                || request.clientMessageId().length() > 64
                || request.speaker() == null
                || request.content() == null || request.content().isBlank()
                || request.content().length() > 2000) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }
}
