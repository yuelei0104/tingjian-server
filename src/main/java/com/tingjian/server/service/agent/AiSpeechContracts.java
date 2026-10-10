package com.tingjian.server.service.agent;

import java.time.Instant;
import java.util.List;

public final class AiSpeechContracts {
    private AiSpeechContracts() {
    }

    public record Message(String speaker, String content) {
    }

    public record GlossaryItem(String term, String alias, String language) {
    }

    public record QuickPhraseItem(String content, String category) {
    }

    public record HistoryItem(String id, String title, String preview) {
    }

    public record UsageMetric(String code, long used, long limit, long remaining) {
    }

    public record UsageItem(String planName, List<UsageMetric> metrics) {
    }

    public record ToolSnapshot(
            List<GlossaryItem> glossary,
            List<QuickPhraseItem> quickPhrases,
            List<HistoryItem> recentHistory,
            UsageItem usage) {
    }

    public record AgentRequest(
            String requestId,
            String userId,
            String sessionId,
            String message,
            String language,
            List<Message> context,
            ToolSnapshot tools) {
    }

    public record AgentResponse(
            String requestId,
            String answer,
            String provider,
            boolean fallback,
            List<String> toolsUsed,
            int contextMessages,
            Instant createdAt) {
    }

    public record InsightRequest(List<Message> messages) {
    }

    public record InsightResponse(
            String summary,
            List<String> highlights,
            List<String> actionItems,
            List<String> keywords,
            String tone,
            String generatedBy,
            boolean fallback) {
    }

    public record ExpressionRequest(
            String action,
            String language,
            String sourceText,
            List<Message> context) {
    }

    public record ExpressionResponse(String suggestion, String provider, boolean fallback) {
    }
}
