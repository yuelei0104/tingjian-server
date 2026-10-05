package com.tingjian.server.entity;

import java.time.LocalDateTime;
import java.util.List;

public record ConversationInsightEntity(
        String conversationId,
        String ownerId,
        String contentHash,
        String summary,
        List<String> highlights,
        List<String> actionItems,
        List<String> keywords,
        String tone,
        String generatedBy,
        int messageCount,
        LocalDateTime updatedAt
) {
}
