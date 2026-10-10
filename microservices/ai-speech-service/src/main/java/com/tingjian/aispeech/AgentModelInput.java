package com.tingjian.aispeech;

import java.util.List;

public record AgentModelInput(
        String systemPrompt,
        String message,
        List<AgentContextMessage> context,
        String toolContext,
        double temperature,
        int maxTokens) {

    public AgentModelInput {
        context = context == null ? List.of() : List.copyOf(context);
        toolContext = toolContext == null ? "" : toolContext;
    }
}
