package com.tingjian.server.dto;

import java.time.LocalDateTime;

public record SessionMessageResponse(
        String id,
        String clientMessageId,
        long sequence,
        String speaker,
        String content,
        LocalDateTime createdAt) {
}
