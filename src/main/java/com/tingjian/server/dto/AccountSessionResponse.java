package com.tingjian.server.dto;

import java.time.LocalDateTime;

public record AccountSessionResponse(
        String id,
        boolean current,
        String deviceName,
        String platform,
        String appVersion,
        String ipAddress,
        LocalDateTime createdAt,
        LocalDateTime lastActiveAt,
        LocalDateTime expiresAt) {
}
