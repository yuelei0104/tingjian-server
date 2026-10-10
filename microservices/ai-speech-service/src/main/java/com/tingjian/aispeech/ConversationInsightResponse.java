package com.tingjian.aispeech;

import java.util.List;

public record ConversationInsightResponse(
        String summary,
        List<String> highlights,
        List<String> actionItems,
        List<String> keywords,
        String tone,
        String generatedBy,
        boolean fallback) {

    public ConversationInsightResponse {
        highlights = highlights == null ? List.of() : List.copyOf(highlights);
        actionItems = actionItems == null ? List.of() : List.copyOf(actionItems);
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
    }
}
