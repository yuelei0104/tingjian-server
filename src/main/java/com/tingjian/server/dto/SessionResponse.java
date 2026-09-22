package com.tingjian.server.dto;

import java.time.LocalDateTime;

public record SessionResponse(
        String id,
        String title,
        String status,
        LocalDateTime startedAt,
        LocalDateTime endedAt) {
}
