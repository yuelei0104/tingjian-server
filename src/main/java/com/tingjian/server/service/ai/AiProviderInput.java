package com.tingjian.server.service.ai;

import com.tingjian.server.dto.AiContextMessageRequest;
import com.tingjian.server.dto.AiSuggestionAction;

import java.util.List;

public record AiProviderInput(
        AiSuggestionAction action,
        String language,
        String sourceText,
        List<AiContextMessageRequest> context
) {
}
