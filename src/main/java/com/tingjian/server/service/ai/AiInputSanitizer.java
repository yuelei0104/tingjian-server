package com.tingjian.server.service.ai;

import com.tingjian.server.dto.AiContextMessageRequest;

import java.util.List;
import java.util.regex.Pattern;

public final class AiInputSanitizer {
    public static final int MAX_CONTEXT_MESSAGES = 8;
    public static final int MAX_MESSAGE_CHARACTERS = 240;

    private static final Pattern EMAIL = Pattern.compile("(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}");
    private static final Pattern BEARER = Pattern.compile("(?i)Bearer\\s+[A-Za-z0-9._~+/=-]{12,}");
    private static final Pattern API_KEY = Pattern.compile("(?i)\\bsk-[A-Za-z0-9_-]{12,}\\b");

    private AiInputSanitizer() {
    }

    public static String sanitize(String value) {
        String text = value == null ? "" : value.strip();
        text = EMAIL.matcher(text).replaceAll("[邮箱]");
        text = BEARER.matcher(text).replaceAll("Bearer [已隐藏]");
        return API_KEY.matcher(text).replaceAll("[密钥]");
    }

    public static List<AiContextMessageRequest> context(List<AiContextMessageRequest> source) {
        int start = Math.max(0, source.size() - MAX_CONTEXT_MESSAGES);
        return source.subList(start, source.size()).stream()
                .map(message -> new AiContextMessageRequest(
                        message.speaker(),
                        clip(sanitize(message.content()), MAX_MESSAGE_CHARACTERS)))
                .toList();
    }

    public static String clip(String value, int maxCharacters) {
        if (value.length() <= maxCharacters) return value;
        return value.substring(0, maxCharacters);
    }
}
