package com.tingjian.server.entity;

import java.time.LocalDateTime;

public record AuthSessionMetadataEntity(
        String sessionId,
        String userId,
        String deviceIdHash,
        String deviceName,
        String platform,
        String appVersion,
        String ipAddress,
        LocalDateTime createdAt) {
}
