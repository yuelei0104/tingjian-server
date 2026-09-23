package com.tingjian.server.dto;

public record PrivacyDeleteResponse(
        int conversations,
        int messages,
        int keywords,
        int glossaryTerms,
        int quickPhrases) {
}
