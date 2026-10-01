package com.tingjian.server.entity;

import java.time.LocalDateTime;

public record ConversationMessageEntity(
        String id,
        String conversationId,
        String clientMessageId,
        long sequence,
        String speaker,
        String content,
        LocalDateTime createdAt) {
}
