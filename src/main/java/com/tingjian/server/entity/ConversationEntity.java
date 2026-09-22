package com.tingjian.server.entity;

import java.time.LocalDateTime;

public record ConversationEntity(
        String id,
        String ownerId,
        String title,
        String status,
        LocalDateTime startedAt,
        LocalDateTime endedAt) {
}
