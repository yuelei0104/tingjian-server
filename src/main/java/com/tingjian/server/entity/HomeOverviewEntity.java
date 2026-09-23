package com.tingjian.server.entity;

public record HomeOverviewEntity(
        long conversationCount,
        long messageCount,
        long totalDurationSeconds) {
}
