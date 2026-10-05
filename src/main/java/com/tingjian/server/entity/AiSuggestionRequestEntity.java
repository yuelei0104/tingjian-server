package com.tingjian.server.entity;

import com.tingjian.server.dto.AiSuggestionAction;

import java.time.LocalDateTime;

public record AiSuggestionRequestEntity(
        String ownerId,
        String clientRequestId,
        String inputHash,
        AiSuggestionAction action,
        String language,
        String suggestion,
        String provider,
        boolean fallback,
        int contextMessages,
        int inputCharacters,
        int outputCharacters,
        LocalDateTime createdAt
) {
}
