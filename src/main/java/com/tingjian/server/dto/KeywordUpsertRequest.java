package com.tingjian.server.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record KeywordUpsertRequest(
        @NotBlank @Size(max = 100) String phrase,
        @NotNull Boolean vibrationEnabled,
        @NotNull @Min(0) @Max(100) Integer priority,
        @NotNull Boolean enabled) {
}
