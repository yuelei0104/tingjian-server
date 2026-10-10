package com.tingjian.server.controller;

import com.tingjian.server.common.ApiResponse;
import com.tingjian.server.common.CurrentUserId;
import com.tingjian.server.dto.AgentChatRequest;
import com.tingjian.server.dto.AgentChatResponse;
import com.tingjian.server.service.agent.AgentService;
import com.tingjian.server.service.agent.AgentStreamListener;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/agent")
public class AgentController {
    private static final long STREAM_TIMEOUT_MILLIS = 50_000L;

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/chat")
    public ApiResponse<AgentChatResponse> chat(
            @CurrentUserId String ownerId,
            @Valid @RequestBody AgentChatRequest request) {
        return ApiResponse.success(agentService.respond(ownerId, request));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @CurrentUserId String ownerId,
            @Valid @RequestBody AgentChatRequest request) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        Thread.startVirtualThread(() -> {
            try {
                emitter.send(SseEmitter.event().name("meta").data(Map.of(
                        "requestId", request.clientRequestId()), MediaType.APPLICATION_JSON));
                AgentChatResponse response = agentService.stream(
                        ownerId, request, new AgentStreamListener() {
                            @Override
                            public void onDelta(String content) {
                                send(emitter, "delta", Map.of("content", content));
                            }

                            @Override
                            public void onReset() {
                                send(emitter, "reset", Map.of("reason", "provider_fallback"));
                            }
                        });
                emitter.send(SseEmitter.event().name("done")
                        .data(response, MediaType.APPLICATION_JSON));
                emitter.complete();
            } catch (Exception exception) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(Map.of(
                            "code", "AI_STREAM_FAILED",
                            "message", "AI 回复暂时不可用，请稍后重试"),
                            MediaType.APPLICATION_JSON));
                } catch (IOException ignored) {
                    // Client disconnected before the terminal event.
                }
                emitter.completeWithError(exception);
            }
        });
        return emitter;
    }

    private static void send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event)
                    .data(data, MediaType.APPLICATION_JSON));
        } catch (IOException exception) {
            throw new IllegalStateException("agent stream client disconnected", exception);
        }
    }
}
