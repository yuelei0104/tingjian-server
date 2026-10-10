package com.tingjian.aispeech;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AgentContextMessage(
        @NotBlank @Pattern(regexp = "SELF|OTHER|ASSISTANT") String speaker,
        @NotBlank @Size(max = 500) String content) {
}
