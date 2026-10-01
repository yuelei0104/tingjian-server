package com.tingjian.server.dto;

public record RealtimeMessageEvent(
        String type,
        String sessionId,
        String clientMessageId,
        SessionMessageResponse message,
        String code,
        String detail) {

    public static RealtimeMessageEvent acknowledged(
            String sessionId, SessionMessageResponse message) {
        return new RealtimeMessageEvent(
                "ACK", sessionId, message.clientMessageId(), message, "OK", "success");
    }

    public static RealtimeMessageEvent message(
            String sessionId, SessionMessageResponse message) {
        return new RealtimeMessageEvent(
                "MESSAGE", sessionId, message.clientMessageId(), message, "OK", "success");
    }

    public static RealtimeMessageEvent error(
            String sessionId, String clientMessageId, String code, String detail) {
        return new RealtimeMessageEvent(
                "ERROR", sessionId, clientMessageId, null, code, detail);
    }
}
