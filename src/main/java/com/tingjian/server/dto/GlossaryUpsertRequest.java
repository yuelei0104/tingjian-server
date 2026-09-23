package com.tingjian.server.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GlossaryUpsertRequest(
        @NotBlank @Size(max = 100) String term,
        @Size(max = 100) String alias,
        @NotBlank @Size(max = 16) String language,
        @NotBlank @Size(max = 40) String category,
        @NotNull @Min(0) @Max(100) Integer priority,
        @NotNull Boolean enabled) {
}
