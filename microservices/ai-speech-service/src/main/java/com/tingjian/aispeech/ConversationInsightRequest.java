package com.tingjian.aispeech;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ConversationInsightRequest(
        @NotNull @Size(min = 1, max = 100) List<@Valid AgentContextMessage> messages) {

    public ConversationInsightRequest {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }
}
