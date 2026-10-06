package com.tingjian.server.service.ai;

public interface AiExpressionProvider {
    AiProviderResult suggest(AiProviderInput input);

    default boolean billable() {
        return false;
    }
}
