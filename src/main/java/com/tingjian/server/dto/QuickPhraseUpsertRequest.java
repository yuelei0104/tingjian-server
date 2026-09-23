package com.tingjian.server.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QuickPhraseUpsertRequest(
        @NotBlank @Size(max = 500) String content,
        @NotBlank @Size(max = 40) String category,
        @NotNull @Min(0) @Max(9999) Integer sortOrder,
        @NotNull Boolean enabled) {
}
