package com.tingjian.server.dto;

import java.time.LocalDateTime;

public record AccountExportResponse(
        String email,
        String phone,
        String displayName,
        LocalDateTime createdAt) {
}
