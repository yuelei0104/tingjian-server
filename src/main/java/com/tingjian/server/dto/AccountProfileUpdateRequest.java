package com.tingjian.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountProfileUpdateRequest(
        @NotBlank @Size(max = 40) String displayName) {
}
