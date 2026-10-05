package com.tingjian.server.entity;

import java.time.LocalDateTime;

public record VerificationCodeEntity(
        String id,
        String channel,
        String destination,
        String purpose,
        String codeHash,
        int failedAttempts,
        LocalDateTime expiresAt,
        LocalDateTime consumedAt,
        LocalDateTime createdAt) {
}
