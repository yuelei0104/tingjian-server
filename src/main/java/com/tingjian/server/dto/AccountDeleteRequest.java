package com.tingjian.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountDeleteRequest(
        @NotBlank @Size(max = 128) String password) {
}
