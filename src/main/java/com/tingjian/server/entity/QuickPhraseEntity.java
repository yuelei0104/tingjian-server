package com.tingjian.server.entity;

import java.time.LocalDateTime;

public record QuickPhraseEntity(
        String id,
        String ownerId,
        String content,
        String category,
        int sortOrder,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
