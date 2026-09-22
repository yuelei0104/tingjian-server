package com.tingjian.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SessionMessageRequest(
        @NotNull Speaker speaker,
        @NotBlank @Size(max = 2000) String content) {

    public enum Speaker {
        OTHER,
        SELF
    }
}
