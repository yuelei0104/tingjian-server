package com.tingjian.aispeech;

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
@RequestMapping("/internal/ai")
public class AgentController {
    private static final long STREAM_TIMEOUT_MILLIS = 45_000L;

    private final AgentOrchestrator orchestrator;
    private final ConversationInsightService insightService;

    public AgentController(
            AgentOrchestrator orchestrator,
            ConversationInsightService insightService) {
        this.orchestrator = orchestrator;
        this.insightService = insightService;
    }

    @PostMapping("/agent/respond")
    public AgentResponse respond(@Valid @RequestBody AgentRequest request) {
        return orchestrator.respond(request);
    }

    @PostMapping(value = "/agent/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody AgentRequest request) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        Thread.startVirtualThread(() -> {
            try {
                emitter.send(SseEmitter.event().name("meta").data(Map.of(
                        "requestId", request.requestId(),
                        "tools", orchestrator.selectTools(request).toolsUsed()),
                        MediaType.APPLICATION_JSON));
                AgentResponse response = orchestrator.stream(request, new AgentStreamSink() {
                    @Override
                    public void delta(String content) {
                        send(emitter, "delta", Map.of("content", content));
                    }

                    @Override
                    public void reset() {
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
                    // The client has already disconnected.
                }
                emitter.completeWithError(exception);
            }
        });
        return emitter;
    }

    @PostMapping("/insights")
    public ConversationInsightResponse insight(
            @Valid @RequestBody ConversationInsightRequest request) {
        return insightService.analyze(request);
    }

    private static void send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event)
                    .data(data, MediaType.APPLICATION_JSON));
        } catch (IOException exception) {
            throw new IllegalStateException("AI stream client disconnected", exception);
        }
    }
}
