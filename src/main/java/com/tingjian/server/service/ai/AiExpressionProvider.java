package com.tingjian.server.service.ai;

public interface AiExpressionProvider {
    AiProviderResult suggest(AiProviderInput input);
}
