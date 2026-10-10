package com.tingjian.server.service.agent;

import com.tingjian.server.dto.AgentChatRequest;
import com.tingjian.server.dto.AgentChatResponse;
import com.tingjian.server.dto.AiContextMessageRequest;
import com.tingjian.server.dto.SessionMessageResponse;
import com.tingjian.server.service.GlossaryService;
import com.tingjian.server.service.HistoryService;
import com.tingjian.server.service.QuickPhraseService;
import com.tingjian.server.service.SessionService;
import com.tingjian.server.service.UsageService;
import com.tingjian.server.service.ai.AiInputSanitizer;
import com.tingjian.server.service.usage.UsageReservationGateway;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AgentService {
    private final SessionService sessionService;
    private final GlossaryService glossaryService;
    private final QuickPhraseService quickPhraseService;
    private final HistoryService historyService;
    private final UsageService usageService;
    private final UsageReservationGateway usageGateway;
    private final AiSpeechAgentGateway agentGateway;

    public AgentService(
            SessionService sessionService,
            GlossaryService glossaryService,
            QuickPhraseService quickPhraseService,
            HistoryService historyService,
            UsageService usageService,
            UsageReservationGateway usageGateway,
            AiSpeechAgentGateway agentGateway) {
        this.sessionService = sessionService;
        this.glossaryService = glossaryService;
        this.quickPhraseService = quickPhraseService;
        this.historyService = historyService;
        this.usageService = usageService;
        this.usageGateway = usageGateway;
        this.agentGateway = agentGateway;
    }

    public AgentChatResponse respond(String ownerId, AgentChatRequest request) {
        AiSpeechContracts.AgentRequest internal = internalRequest(ownerId, request);
        UsageReservationGateway.Reservation reservation = reserve(ownerId, request.clientRequestId());
        try {
            AiSpeechContracts.AgentResponse result = agentGateway.respond(internal);
            finishUsage(reservation, result.fallback());
            return toResponse(result);
        } catch (RuntimeException exception) {
            usageGateway.release(reservation);
            return fallback(request, internal.context().size());
        }
    }

    public AgentChatResponse stream(
            String ownerId,
            AgentChatRequest request,
            AgentStreamListener listener) {
        AiSpeechContracts.AgentRequest internal = internalRequest(ownerId, request);
        UsageReservationGateway.Reservation reservation = reserve(ownerId, request.clientRequestId());
        try {
            AiSpeechContracts.AgentResponse result = agentGateway.stream(internal, listener);
            finishUsage(reservation, result.fallback());
            return toResponse(result);
        } catch (RuntimeException exception) {
            usageGateway.release(reservation);
            AgentChatResponse fallback = fallback(request, internal.context().size());
            listener.onReset();
            emitLocal(fallback.answer(), listener);
            return fallback;
        }
    }

    private AiSpeechContracts.AgentRequest internalRequest(
            String ownerId, AgentChatRequest request) {
        List<AiSpeechContracts.Message> context = context(ownerId, request);
        return new AiSpeechContracts.AgentRequest(
                request.clientRequestId(), ownerId, normalize(request.sessionId()),
                clean(request.message(), 1000), request.language(), context, tools(ownerId));
    }

    private List<AiSpeechContracts.Message> context(String ownerId, AgentChatRequest request) {
        if (request.sessionId() != null && !request.sessionId().isBlank()) {
            List<SessionMessageResponse> stored = sessionService.messages(ownerId, request.sessionId());
            int start = Math.max(0, stored.size() - 30);
            return stored.subList(start, stored.size()).stream()
                    .map(message -> new AiSpeechContracts.Message(
                            message.speaker(), clean(message.content(), 500)))
                    .toList();
        }
        int start = Math.max(0, request.context().size() - 30);
        return request.context().subList(start, request.context().size()).stream()
                .map(message -> new AiSpeechContracts.Message(
                        message.speaker(), clean(message.content(), 500)))
                .toList();
    }

    private AiSpeechContracts.ToolSnapshot tools(String ownerId) {
        var glossary = glossaryService.list(ownerId).stream()
                .filter(item -> item.enabled())
                .limit(30)
                .map(item -> new AiSpeechContracts.GlossaryItem(
                        clean(item.term(), 80), cleanNullable(item.alias(), 80),
                        clean(item.language(), 30)))
                .toList();
        var quickPhrases = quickPhraseService.list(ownerId).stream()
                .filter(item -> item.enabled())
                .limit(30)
                .map(item -> new AiSpeechContracts.QuickPhraseItem(
                        clean(item.content(), 160), clean(item.category(), 40)))
                .toList();
        var history = historyService.search(ownerId, "", 0, 5).items().stream()
                .map(item -> new AiSpeechContracts.HistoryItem(
                        item.id(), clean(item.title(), 80), cleanNullable(item.preview(), 180)))
                .toList();
        var usage = usageService.get(ownerId);
        var metrics = usage.metrics().stream()
                .map(metric -> new AiSpeechContracts.UsageMetric(
                        metric.code(), metric.used(), metric.limit(), metric.remaining()))
                .toList();
        return new AiSpeechContracts.ToolSnapshot(
                glossary, quickPhrases, history,
                new AiSpeechContracts.UsageItem(usage.planName(), metrics));
    }

    private UsageReservationGateway.Reservation reserve(String ownerId, String requestId) {
        if (!agentGateway.enabled()) return UsageReservationGateway.Reservation.unmetered();
        return usageGateway.reserve(
                ownerId, UsageReservationGateway.Metric.AI_REQUESTS, 1, "agent:" + requestId);
    }

    private void finishUsage(UsageReservationGateway.Reservation reservation, boolean fallback) {
        if (!reservation.metered()) return;
        if (fallback) usageGateway.release(reservation);
        else usageGateway.commit(reservation);
    }

    private static AgentChatResponse toResponse(AiSpeechContracts.AgentResponse response) {
        return new AgentChatResponse(
                response.requestId(), response.answer(), response.provider(), response.fallback(),
                response.toolsUsed(), response.contextMessages(), response.createdAt());
    }

    private static AgentChatResponse fallback(AgentChatRequest request, int contextMessages) {
        String answer = request.message().contains("?") || request.message().contains("？")
                ? "我已经收到这个问题，但当前云端 AI 暂不可用。您可以稍后重试。"
                : "好的，我已经记录这段内容。云端 AI 恢复后可以继续为您分析。";
        return new AgentChatResponse(
                request.clientRequestId(), answer, "LOCAL_AGENT_FALLBACK", true,
                List.of(), contextMessages, Instant.now());
    }

    private static void emitLocal(String answer, AgentStreamListener listener) {
        for (int index = 0; index < answer.length(); index += 8) {
            listener.onDelta(answer.substring(index, Math.min(answer.length(), index + 8)));
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static String clean(String value, int limit) {
        return AiInputSanitizer.clip(AiInputSanitizer.sanitize(value), limit);
    }

    private static String cleanNullable(String value, int limit) {
        return value == null ? null : clean(value, limit);
    }
}
