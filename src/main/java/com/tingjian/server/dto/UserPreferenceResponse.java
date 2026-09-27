package com.tingjian.server.dto;

import java.time.LocalDateTime;

public record UserPreferenceResponse(
        boolean configured,
        boolean largeText,
        String voiceMode,
        String voiceStyle,
        double ttsSpeed,
        String recognitionLanguage,
        boolean keywordVibration,
        boolean keywordHighlight,
        boolean autoSummary,
        LocalDateTime updatedAt) {
}
