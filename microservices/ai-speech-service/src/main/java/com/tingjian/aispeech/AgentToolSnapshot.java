package com.tingjian.aispeech;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AgentToolSnapshot(
        @NotNull @Size(max = 30) List<@Valid GlossaryItem> glossary,
        @NotNull @Size(max = 30) List<@Valid QuickPhraseItem> quickPhrases,
        @NotNull @Size(max = 10) List<@Valid HistoryItem> recentHistory,
        UsageItem usage) {

    public AgentToolSnapshot {
        glossary = glossary == null ? List.of() : List.copyOf(glossary);
        quickPhrases = quickPhrases == null ? List.of() : List.copyOf(quickPhrases);
        recentHistory = recentHistory == null ? List.of() : List.copyOf(recentHistory);
    }

    public record GlossaryItem(String term, String alias, String language) {
    }

    public record QuickPhraseItem(String content, String category) {
    }

    public record HistoryItem(String id, String title, String preview) {
    }

    public record UsageItem(String planName, List<UsageMetric> metrics) {
        public UsageItem {
            metrics = metrics == null ? List.of() : List.copyOf(metrics);
        }
    }

    public record UsageMetric(String code, long used, long limit, long remaining) {
    }
}
