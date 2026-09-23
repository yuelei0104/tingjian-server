package com.tingjian.server.dto;

import java.time.LocalDateTime;

public record QuickPhraseResponse(
        String id,
        String content,
        String category,
        int sortOrder,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
