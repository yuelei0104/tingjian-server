package com.tingjian.server.entity;

import java.time.LocalDateTime;

public record UserPreferenceEntity(
        String ownerId,
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
