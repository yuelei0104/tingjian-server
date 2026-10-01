package com.tingjian.server.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ConversationExportResponse(
        String id,
        String title,
        String status,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        List<MessageExportResponse> messages) {
}
