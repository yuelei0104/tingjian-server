package com.tingjian.aispeech;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationInsightServiceTests {
    @Test
    void parsesStructuredModelInsight() {
        AgentModelProvider provider = input -> new AgentModelResult("""
                {"summary":"讨论了发布计划","highlights":["测试通过"],
                "actionItems":["明天提交"],"keywords":["发布"],"tone":"积极"}
                """, "QWEN:test", false);
        ConversationInsightService service = new ConversationInsightService(
                provider, new ObjectMapper());

        ConversationInsightResponse result = service.analyze(new ConversationInsightRequest(
                List.of(new AgentContextMessage("OTHER", "测试通过，明天提交。"))));

        assertThat(result.summary()).isEqualTo("讨论了发布计划");
        assertThat(result.actionItems()).containsExactly("明天提交");
        assertThat(result.generatedBy()).isEqualTo("QWEN:test");
        assertThat(result.fallback()).isFalse();
    }

    @Test
    void malformedModelOutputUsesLocalInsight() {
        AgentModelProvider provider = input -> new AgentModelResult("not-json", "QWEN:test", false);
        ConversationInsightService service = new ConversationInsightService(
                provider, new ObjectMapper());

        ConversationInsightResponse result = service.analyze(new ConversationInsightRequest(
                List.of(new AgentContextMessage("OTHER", "请明天提交测试报告。"))));

        assertThat(result.generatedBy()).isEqualTo("LOCAL_INSIGHT_FALLBACK");
        assertThat(result.actionItems()).contains("请明天提交测试报告。");
        assertThat(result.fallback()).isTrue();
    }
}
