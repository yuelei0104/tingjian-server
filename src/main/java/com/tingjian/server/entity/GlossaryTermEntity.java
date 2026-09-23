package com.tingjian.server.entity;

import java.time.LocalDateTime;

public record GlossaryTermEntity(
        String id,
        String ownerId,
        String term,
        String alias,
        String language,
        String category,
        int priority,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
