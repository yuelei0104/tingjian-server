package com.tingjian.server.dto;

import jakarta.validation.constraints.Size;

public record SessionCreateRequest(@Size(max = 80) String title) {
}
