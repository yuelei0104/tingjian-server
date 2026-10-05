package com.tingjian.server.service.insight;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalConversationIntelligenceProviderTests {
    private final LocalConversationIntelligenceProvider provider =
            new LocalConversationIntelligenceProvider();

    @Test
    void extractsHighlightsTasksKeywordsAndTone() {
        var result = provider.analyze(new ConversationInsightInput(List.of(
                new ConversationInsightMessage("OTHER", "请明天提交项目报告"),
                new ConversationInsightMessage("SELF", "好的，我会完成项目报告，谢谢"))));

        assertEquals(2, result.highlights().size());
        assertFalse(result.actionItems().isEmpty());
        assertFalse(result.keywords().isEmpty());
        assertEquals("积极", result.tone());
        assertTrue(result.summary().contains("待办"));
    }

    @Test
    void emptyConversationProducesSafeResult() {
        var result = provider.analyze(new ConversationInsightInput(List.of()));

        assertTrue(result.summary().contains("没有"));
        assertTrue(result.actionItems().isEmpty());
    }
}
