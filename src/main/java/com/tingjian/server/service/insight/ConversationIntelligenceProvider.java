package com.tingjian.server.service.insight;

public interface ConversationIntelligenceProvider {
    ConversationInsightResult analyze(ConversationInsightInput input);
}
