package com.tingjian.aispeech;

import java.util.List;

final class AgentText {
    private AgentText() {
    }

    static String sanitize(String value, int maxLength) {
        if (value == null) return "";
        String clean = value.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "")
                .replaceAll("(?i)(bearer\\s+)[A-Za-z0-9._~+/-]+=*", "$1[REDACTED]")
                .replaceAll("(?i)(api[-_ ]?key\\s*[:=]\\s*)\\S+", "$1[REDACTED]")
                .strip();
        return clean.length() <= maxLength ? clean : clean.substring(0, maxLength);
    }

    static List<AgentContextMessage> context(List<AgentContextMessage> value) {
        if (value == null) return List.of();
        int start = Math.max(0, value.size() - 30);
        return value.subList(start, value.size()).stream()
                .map(message -> new AgentContextMessage(
                        message.speaker(), sanitize(message.content(), 500)))
                .filter(message -> !message.content().isBlank())
                .toList();
    }
}
