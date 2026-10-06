package com.tingjian.server.service.ai;

import com.tingjian.server.dto.AiContextMessageRequest;
import com.tingjian.server.dto.AiSuggestionAction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QwenAiExpressionProviderTests {
    @Test
    void buildsBoundedRoleAwareMessages() {
        var input = new AiProviderInput(
                AiSuggestionAction.REPLY,
                "中文",
                "",
                List.of(
                        new AiContextMessageRequest("OTHER", "你明天方便吗？"),
                        new AiContextMessageRequest("SELF", "下午可以")));

        var messages = QwenAiExpressionProvider.messages(input);

        assertEquals("system", messages.getFirst().role());
        assertEquals("user", messages.get(1).role());
        assertEquals("assistant", messages.get(2).role());
        assertTrue(messages.getLast().content().contains("简短"));
    }
}
