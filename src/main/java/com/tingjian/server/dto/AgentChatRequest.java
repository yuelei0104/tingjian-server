package com.tingjian.server.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AgentChatRequest(
        @NotBlank @Size(max = 64) String clientRequestId,
        @Size(max = 36) String sessionId,
        @NotBlank @Size(max = 1000) String message,
        @NotBlank @Pattern(regexp = "中英混合|中文|English") String language,
        @NotNull @Size(max = 30) List<@Valid AiContextMessageRequest> context) {

    public AgentChatRequest {
        context = context == null ? List.of() : List.copyOf(context);
    }
}
