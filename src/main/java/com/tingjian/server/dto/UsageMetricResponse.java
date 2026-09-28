package com.tingjian.server.dto;

public record UsageMetricResponse(
        String code,
        String name,
        String unit,
        long used,
        long limit,
        long remaining,
        double progress,
        String description) {
}
