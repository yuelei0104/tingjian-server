package com.tingjian.server.dto;

import java.time.LocalDateTime;

public record AccountSessionResponse(
        String id,
        LocalDateTime createdAt,
        LocalDateTime lastActiveAt,
        LocalDateTime expiresAt) {
}
