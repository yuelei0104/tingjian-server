package com.tingjian.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SessionRenameRequest(
        @NotBlank @Size(max = 80) String title) {
}
