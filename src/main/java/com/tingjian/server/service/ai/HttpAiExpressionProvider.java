package com.tingjian.server.service.ai;

import com.tingjian.server.service.agent.AiSpeechAgentGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tingjian.ai.provider", havingValue = "remote")
public class HttpAiExpressionProvider implements AiExpressionProvider {
    private final AiSpeechAgentGateway gateway;

    public HttpAiExpressionProvider(AiSpeechAgentGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public AiProviderResult suggest(AiProviderInput input) {
        return gateway.expression(input);
    }

    @Override
    public boolean billable() {
        return true;
    }
}
