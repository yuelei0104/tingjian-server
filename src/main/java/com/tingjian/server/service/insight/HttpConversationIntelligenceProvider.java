package com.tingjian.server.service.insight;

import com.tingjian.server.service.agent.AiSpeechAgentGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tingjian.insight.provider", havingValue = "remote")
public class HttpConversationIntelligenceProvider implements ConversationIntelligenceProvider {
    private final AiSpeechAgentGateway gateway;

    public HttpConversationIntelligenceProvider(AiSpeechAgentGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public ConversationInsightResult analyze(ConversationInsightInput input) {
        return gateway.insight(input);
    }
}
