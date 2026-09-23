package com.tingjian.server.entity;

import java.time.LocalDateTime;

public record AuthSessionEntity(
        String id,
        String userId,
        String accessTokenHash,
        String refreshTokenHash,
        LocalDateTime accessExpiresAt,
        LocalDateTime refreshExpiresAt,
        LocalDateTime revokedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
