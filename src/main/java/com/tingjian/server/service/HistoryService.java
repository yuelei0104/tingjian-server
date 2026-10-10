package com.tingjian.server.service;

import com.tingjian.server.dao.HistoryDao;
import com.tingjian.server.dao.ConversationInsightDao;
import com.tingjian.server.dto.HistoryItemResponse;
import com.tingjian.server.dto.HistoryListResponse;
import com.tingjian.server.dto.HistorySummaryResponse;
import com.tingjian.server.dto.SessionDetailResponse;
import com.tingjian.server.entity.HistorySummaryEntity;
import com.tingjian.server.entity.ConversationInsightEntity;
import com.tingjian.server.service.ai.AiInputSanitizer;
import com.tingjian.server.service.insight.ConversationInsightInput;
import com.tingjian.server.service.insight.ConversationInsightMessage;
import com.tingjian.server.service.insight.ConversationInsightResult;
import com.tingjian.server.service.insight.ConversationIntelligenceProvider;
import com.tingjian.server.service.insight.LocalConversationIntelligenceProvider;
import com.tingjian.server.util.TokenGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class HistoryService {
    private static final long PROVIDER_TIMEOUT_SECONDS = 20;
    private final HistoryDao historyDao;
    private final SessionService sessionService;
    private final ConversationInsightDao insightDao;
    private final ConversationIntelligenceProvider intelligenceProvider;

    public HistoryService(
            HistoryDao historyDao,
            SessionService sessionService,
            ConversationInsightDao insightDao,
            ConversationIntelligenceProvider intelligenceProvider) {
        this.historyDao = historyDao;
        this.sessionService = sessionService;
        this.insightDao = insightDao;
        this.intelligenceProvider = intelligenceProvider;
    }

    public HistoryListResponse search(String ownerId, String keyword, int page, int size) {
        long offset = (long) page * size;
        long total = historyDao.count(ownerId, keyword);
        var items = historyDao.search(ownerId, keyword, size, offset).stream()
                .map(HistoryService::toResponse)
                .toList();
        return new HistoryListResponse(items, page, size, total, offset + items.size() < total);
    }

    public SessionDetailResponse detail(String ownerId, String id) {
        return new SessionDetailResponse(
                sessionService.get(ownerId, id),
                sessionService.messages(ownerId, id));
    }

    public HistorySummaryResponse summarize(String ownerId, String id) {
        var detail = detail(ownerId, id);
        List<ConversationInsightMessage> allMessages = detail.messages().stream()
                .map(message -> new ConversationInsightMessage(
                        message.speaker(),
                        AiInputSanitizer.clip(
                                AiInputSanitizer.sanitize(message.content()), 300)))
                .filter(message -> !message.content().isBlank())
                .toList();
        if (allMessages.isEmpty()) {
            return new HistorySummaryResponse(id, "这段会话暂时没有可供整理的文字。", 0,
                    "LOCAL_INSIGHT_V1", List.of(), List.of(), List.of(), "暂无", false);
        }

        int start = Math.max(0, allMessages.size() - 100);
        List<ConversationInsightMessage> messages = allMessages.subList(start, allMessages.size());
        String contentHash = contentHash(messages);
        var cached = insightDao.find(id, ownerId).orElse(null);
        if (cached != null && cached.contentHash().equals(contentHash)) {
            return toResponse(cached, true);
        }

        ConversationInsightResult result = analyze(new ConversationInsightInput(messages));
        var entity = new ConversationInsightEntity(
                id, ownerId, contentHash, result.summary(), result.highlights(),
                result.actionItems(), result.keywords(), result.tone(), result.generatedBy(),
                allMessages.size(), LocalDateTime.now(ZoneOffset.UTC));
        insightDao.save(entity);
        return toResponse(entity, false);
    }

    @Transactional
    public void delete(String ownerId, String id) {
        if (!historyDao.existsForUpdate(id, ownerId)) {
            // DELETE 保持幂等：响应丢失后客户端可以安全重试，也不暴露其他账号的数据。
            return;
        }
        historyDao.deleteMessages(id);
        historyDao.deleteConversation(id, ownerId);
    }

    private static HistoryItemResponse toResponse(HistorySummaryEntity entity) {
        return new HistoryItemResponse(
                entity.id(), entity.title(), entity.status(), entity.startedAt(), entity.endedAt(),
                entity.messageCount(), entity.preview());
    }

    private ConversationInsightResult analyze(ConversationInsightInput input) {
        try {
            ConversationInsightResult result = CompletableFuture
                    .supplyAsync(() -> intelligenceProvider.analyze(input))
                    .orTimeout(PROVIDER_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .join();
            if (result == null || result.summary() == null || result.summary().isBlank()) {
                return LocalConversationIntelligenceProvider.fallback(input);
            }
            return result;
        } catch (RuntimeException ignored) {
            return LocalConversationIntelligenceProvider.fallback(input);
        }
    }

    private static String contentHash(List<ConversationInsightMessage> messages) {
        StringBuilder canonical = new StringBuilder();
        messages.forEach(message -> canonical.append(message.speaker())
                .append(':').append(message.content()).append('\n'));
        return TokenGenerator.hash(canonical.toString());
    }

    private static HistorySummaryResponse toResponse(
            ConversationInsightEntity entity, boolean cached) {
        return new HistorySummaryResponse(
                entity.conversationId(), entity.summary(), entity.messageCount(),
                entity.generatedBy(), entity.highlights(), entity.actionItems(),
                entity.keywords(), entity.tone(), cached);
    }
}
