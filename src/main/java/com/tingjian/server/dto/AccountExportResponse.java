package com.tingjian.server.dto;

import java.time.LocalDateTime;

public record AccountExportResponse(
        String email,
        String displayName,
        LocalDateTime createdAt) {
}
