package com.tingjian.aispeech;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AgentRequest(
        @NotBlank @Size(max = 64) String requestId,
        @NotBlank @Size(max = 64) String userId,
        @Size(max = 36) String sessionId,
        @NotBlank @Size(max = 1000) String message,
        @NotBlank @Pattern(regexp = "中英混合|中文|English") String language,
        @NotNull @Size(max = 30) List<@Valid AgentContextMessage> context,
        @NotNull @Valid AgentToolSnapshot tools) {

    public AgentRequest {
        context = context == null ? List.of() : List.copyOf(context);
    }
}
