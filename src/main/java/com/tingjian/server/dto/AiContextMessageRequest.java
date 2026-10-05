package com.tingjian.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AiContextMessageRequest(
        @NotBlank @Pattern(regexp = "SELF|OTHER") String speaker,
        @NotBlank @Size(max = 500) String content
) {
}
