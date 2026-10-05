package com.tingjian.server.service.ai;

import com.tingjian.server.dto.AiContextMessageRequest;
import com.tingjian.server.dto.AiSuggestionAction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiInputSanitizerTests {
    @Test
    void redactsSecretsAndKeepsOnlyRecentContext() {
        var messages = IntStream.range(0, 10)
                .mapToObj(index -> new AiContextMessageRequest(
                        index % 2 == 0 ? "SELF" : "OTHER",
                        "message-" + index + " user@example.com sk-abcdefghijklmnop"))
                .toList();

        var sanitized = AiInputSanitizer.context(messages);

        assertEquals(8, sanitized.size());
        assertTrue(sanitized.getFirst().content().startsWith("message-2"));
        assertTrue(sanitized.getLast().content().contains("[邮箱]"));
        assertTrue(sanitized.getLast().content().contains("[密钥]"));
        assertFalse(sanitized.getLast().content().contains("example.com"));
    }

    @Test
    void localProviderCanGenerateReplyAndRewrite() {
        var provider = new LocalTemplateAiProvider();
        var context = List.of(new AiContextMessageRequest("OTHER", "你明天有时间吗？"));

        var reply = provider.suggest(new AiProviderInput(
                AiSuggestionAction.REPLY, "中文", "", context));
        var polite = provider.suggest(new AiProviderInput(
                AiSuggestionAction.POLITE, "中文", "你稍等", List.of()));

        assertTrue(reply.suggestion().contains("回复"));
        assertTrue(polite.suggestion().contains("您"));
        assertTrue(polite.fallback());
    }
}
