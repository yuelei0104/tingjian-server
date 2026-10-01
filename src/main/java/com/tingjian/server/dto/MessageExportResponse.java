package com.tingjian.server.dto;

import java.time.LocalDateTime;

public record MessageExportResponse(
        String speaker,
        String content,
        LocalDateTime createdAt) {
}
