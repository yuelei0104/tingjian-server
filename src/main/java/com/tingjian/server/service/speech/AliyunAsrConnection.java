package com.tingjian.server.service.speech;

import com.tingjian.server.config.CloudAsrProperties;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AliyunAsrConnection implements WebSocket.Listener {
    public interface Listener {
        void onReady();

        void onResult(String text, boolean sentenceEnd);

        void onFailure(String code, String message);

        void onComplete();
    }

    private final CloudAsrProperties properties;
    private final ObjectMapper mapper;
    private final String language;
    private final Listener listener;
    private final String taskId = UUID.randomUUID().toString();
    private final StringBuilder textBuffer = new StringBuilder();
    private final AtomicBoolean finished = new AtomicBoolean();
    private volatile WebSocket socket;
    private volatile boolean ready;

    public AliyunAsrConnection(
            CloudAsrProperties properties,
            ObjectMapper mapper,
            String language,
            Listener listener) {
        this.properties = properties;
        this.mapper = mapper;
        this.language = language;
        this.listener = listener;
    }

    public void connect() {
        HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build()
                .newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .header("Authorization", "Bearer " + properties.apiKey())
                .header("User-Agent", "Tingjian-Server/1.0")
                .buildAsync(URI.create(properties.websocketUrl()), this)
                .exceptionally(error -> {
                    fail("CLOUD_CONNECT_FAILED", "云端语音识别连接失败");
                    return null;
                });
    }

    public boolean isReady() {
        return ready && !finished.get();
    }

    public void sendAudio(byte[] audio) {
        WebSocket current = socket;
        if (!isReady() || current == null || audio.length == 0) return;
        current.sendBinary(ByteBuffer.wrap(audio), true)
                .exceptionally(error -> {
                    fail("CLOUD_SEND_FAILED", "音频上传失败");
                    return null;
                });
    }

    public void finish() {
        if (!finished.compareAndSet(false, true)) return;
        WebSocket current = socket;
        if (current == null) return;
        try {
            current.sendText(AliyunAsrProtocol.finishTask(mapper, taskId), true);
        } catch (Exception exception) {
            current.abort();
        }
    }

    public void abort() {
        finished.set(true);
        WebSocket current = socket;
        if (current != null) current.abort();
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        this.socket = webSocket;
        webSocket.request(1);
        try {
            webSocket.sendText(AliyunAsrProtocol.runTask(
                    mapper, taskId, properties.model(), language,
                    properties.maxSentenceSilence()), true);
        } catch (Exception exception) {
            fail("CLOUD_PROTOCOL_ERROR", "无法启动云端语音识别");
        }
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        textBuffer.append(data);
        if (last) {
            String message = textBuffer.toString();
            textBuffer.setLength(0);
            handleEvent(message);
        }
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        ready = false;
        if (!finished.get()) fail("CLOUD_CLOSED", "云端识别连接已断开");
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        ready = false;
        fail("CLOUD_ERROR", "云端语音识别暂时不可用");
    }

    private void handleEvent(String json) {
        try {
            AliyunAsrProtocol.CloudEvent event = mapper.readValue(
                    json, AliyunAsrProtocol.CloudEvent.class);
            if (event == null || event.header() == null) return;
            switch (event.header().event()) {
                case "task-started" -> {
                    ready = true;
                    listener.onReady();
                }
                case "result-generated" -> emitResult(event);
                case "task-finished" -> {
                    ready = false;
                    listener.onComplete();
                    closeNormally();
                }
                case "task-failed" -> fail(
                        safe(event.header().error_code(), "CLOUD_TASK_FAILED"),
                        safe(event.header().error_message(), "云端识别任务失败"));
                default -> {
                    // Ignore future provider events that do not affect recognition.
                }
            }
        } catch (Exception exception) {
            fail("CLOUD_RESPONSE_INVALID", "云端识别响应格式不正确");
        }
    }

    private void emitResult(AliyunAsrProtocol.CloudEvent event) {
        if (event.payload() == null || event.payload().output() == null
                || event.payload().output().sentence() == null) return;
        AliyunAsrProtocol.Sentence sentence = event.payload().output().sentence();
        if (sentence.heartbeat() || sentence.text() == null || sentence.text().isBlank()) return;
        listener.onResult(sentence.text().strip(), sentence.sentence_end());
    }

    private void fail(String code, String message) {
        ready = false;
        if (finished.compareAndSet(false, true)) listener.onFailure(code, message);
        WebSocket current = socket;
        if (current != null) current.abort();
    }

    private void closeNormally() {
        WebSocket current = socket;
        if (current != null) current.sendClose(WebSocket.NORMAL_CLOSURE, "done");
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
