package com.tingjian.server.dto;

import java.time.LocalDateTime;

public record AiSuggestionResponse(
        String clientRequestId,
        String suggestion,
        AiSuggestionAction action,
        String language,
        String provider,
        boolean fallback,
        int contextMessages,
        int inputCharacters,
        int outputCharacters,
        LocalDateTime createdAt
) {
}
