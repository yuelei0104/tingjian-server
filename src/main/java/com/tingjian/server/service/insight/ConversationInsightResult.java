package com.tingjian.server.service.insight;

import java.util.List;

public record ConversationInsightResult(
        String summary,
        List<String> highlights,
        List<String> actionItems,
        List<String> keywords,
        String tone,
        String generatedBy
) {
}
