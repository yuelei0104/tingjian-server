package com.tingjian.server.dto;

import java.time.LocalDateTime;

public record GlossaryResponse(
        String id,
        String term,
        String alias,
        String language,
        String category,
        int priority,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
