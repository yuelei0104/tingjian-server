package com.tingjian.server.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PrivacyExportResponse(
        LocalDateTime exportedAt,
        AccountExportResponse account,
        List<ConversationExportResponse> conversations,
        List<KeywordResponse> keywords,
        List<GlossaryResponse> glossaryTerms,
        List<QuickPhraseResponse> quickPhrases,
        UserPreferenceResponse preferences) {
}
