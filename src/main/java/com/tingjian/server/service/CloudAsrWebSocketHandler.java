package com.tingjian.server.service;

import com.tingjian.server.config.CloudAsrProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import com.tingjian.server.service.speech.AliyunAsrConnection;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CloudAsrWebSocketHandler extends BinaryWebSocketHandler {
    private static final int MAX_AUDIO_FRAME_BYTES = 32 * 1024;

    private final CloudAsrProperties properties;
    private final ObjectMapper mapper;
    private final Map<String, AliyunAsrConnection> connections = new ConcurrentHashMap<>();

    public CloudAsrWebSocketHandler(CloudAsrProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (!properties.enabled()) {
            send(session, event("ERROR", Map.of(
                    "code", "CLOUD_NOT_CONFIGURED",
                    "message", "云端识别尚未配置，将使用设备识别")));
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        AliyunAsrConnection connection = new AliyunAsrConnection(
                properties, mapper, queryLanguage(session.getUri()),
                new AliyunAsrConnection.Listener() {
                    @Override
                    public void onReady() {
                        send(session, event("READY", Map.of()));
                    }

                    @Override
                    public void onResult(String text, boolean sentenceEnd) {
                        send(session, event(sentenceEnd ? "FINAL" : "PARTIAL", Map.of("text", text)));
                    }

                    @Override
                    public void onFailure(String code, String message) {
                        send(session, event("ERROR", Map.of("code", code, "message", message)));
                        close(session, CloseStatus.SERVER_ERROR);
                    }

                    @Override
                    public void onComplete() {
                        send(session, event("COMPLETE", Map.of()));
                        close(session, CloseStatus.NORMAL);
                    }
                });
        connections.put(session.getId(), connection);
        connection.connect();
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        if (message.getPayloadLength() > MAX_AUDIO_FRAME_BYTES) {
            send(session, event("ERROR", Map.of(
                    "code", "AUDIO_FRAME_TOO_LARGE",
                    "message", "音频分片过大")));
            close(session, CloseStatus.TOO_BIG_TO_PROCESS);
            return;
        }
        AliyunAsrConnection connection = connections.get(session.getId());
        if (connection == null || !connection.isReady()) return;
        ByteBuffer payload = message.getPayload();
        byte[] audio = new byte[payload.remaining()];
        payload.get(audio);
        connection.sendAudio(audio);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        AliyunAsrConnection connection = connections.get(session.getId());
        if (connection == null) return;
        if ("FINISH".equalsIgnoreCase(message.getPayload().strip())) connection.finish();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        AliyunAsrConnection connection = connections.remove(session.getId());
        if (connection != null) connection.abort();
    }

    private Map<String, Object> event(String type, Map<String, Object> data) {
        return Map.of("type", type, "data", data);
    }

    private void send(WebSocketSession session, Map<String, Object> event) {
        if (!session.isOpen()) return;
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(mapper.writeValueAsString(event)));
            }
        } catch (Exception ignored) {
            // The Android client reconnects or falls back to device speech recognition.
        }
    }

    private static String queryLanguage(URI uri) {
        if (uri == null || uri.getRawQuery() == null) return "中英混合";
        for (String part : uri.getRawQuery().split("&")) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2 && "language".equals(pair[0])) {
                return java.net.URLDecoder.decode(pair[1], java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        return "中英混合";
    }

    private static void close(WebSocketSession session, CloseStatus status) {
        try {
            if (session.isOpen()) session.close(status);
        } catch (Exception ignored) {
            // No resources remain after the close callback removes the cloud connection.
        }
    }
}
