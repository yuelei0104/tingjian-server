package com.tingjian.aispeech;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentOrchestratorTests {
    @Test
    void selectsOnlyRelevantReadOnlyToolsAndReturnsModelAnswer() {
        AgentModelProvider provider = input -> {
            assertThat(input.toolContext()).contains("术语表工具结果", "ASR = 语音转文字", "剩余 18");
            return new AgentModelResult("这是结合术语和额度生成的回答。", "TEST_MODEL", false);
        };
        AgentOrchestrator service = new AgentOrchestrator(provider, new AgentToolRegistry());

        AgentResponse response = service.respond(request("请查看专业词术语和剩余额度"));

        assertThat(response.answer()).isEqualTo("这是结合术语和额度生成的回答。");
        assertThat(response.toolsUsed()).containsExactly("lookupGlossary", "getUsage");
        assertThat(response.fallback()).isFalse();
    }

    @Test
    void providerFailureFallsBackAndResetsStream() {
        AgentModelProvider provider = input -> {
            throw new IllegalStateException("offline");
        };
        AgentOrchestrator service = new AgentOrchestrator(provider, new AgentToolRegistry());
        StringBuilder streamed = new StringBuilder();
        boolean[] reset = {false};

        AgentResponse response = service.stream(request("能帮我回答吗？"), new AgentStreamSink() {
            @Override
            public void delta(String content) {
                streamed.append(content);
            }

            @Override
            public void reset() {
                reset[0] = true;
            }
        });

        assertThat(reset[0]).isTrue();
        assertThat(streamed).isNotEmpty();
        assertThat(response.fallback()).isTrue();
        assertThat(response.provider()).isEqualTo("LOCAL_AGENT_FALLBACK");
    }

    private static AgentRequest request(String message) {
        return new AgentRequest(
                "request-123", "user-123", "session-123", message, "中文",
                List.of(new AgentContextMessage("OTHER", "请解释这个专业词")),
                new AgentToolSnapshot(
                        List.of(new AgentToolSnapshot.GlossaryItem("ASR", "语音转文字", "中文")),
                        List.of(new AgentToolSnapshot.QuickPhraseItem("请稍等", "日常")),
                        List.of(new AgentToolSnapshot.HistoryItem("history-1", "上次会话", "讨论额度")),
                        new AgentToolSnapshot.UsageItem("内测版", List.of(
                                new AgentToolSnapshot.UsageMetric("AI_REQUESTS", 2, 20, 18)))));
    }
}
