package com.tingjian.aispeech;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tingjian.ai.provider", havingValue = "local", matchIfMissing = true)
public class LocalAgentModelProvider implements AgentModelProvider {
    @Override
    public AgentModelResult complete(AgentModelInput input) {
        return fallback(input);
    }

    static AgentModelResult fallback(AgentModelInput input) {
        String message = AgentText.sanitize(input.message(), 500);
        String answer;
        if (!input.toolContext().isBlank()) {
            answer = "我已查询到相关个人数据。当前云端 AI 不可用，请稍后重试；以下信息可供参考：\n"
                    + AgentText.sanitize(input.toolContext(), 700);
        } else if (message.contains("?") || message.contains("？")) {
            answer = "我已经收到这个问题，但当前云端 AI 暂不可用。您可以稍后重试。";
        } else {
            answer = "好的，我已经记录这段内容。云端 AI 恢复后可以继续为您分析。";
        }
        return new AgentModelResult(answer, "LOCAL_AGENT_FALLBACK", true);
    }
}
