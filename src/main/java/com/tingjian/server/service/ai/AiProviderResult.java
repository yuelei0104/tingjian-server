package com.tingjian.server.service.ai;

public record AiProviderResult(String suggestion, String provider, boolean fallback) {
}
