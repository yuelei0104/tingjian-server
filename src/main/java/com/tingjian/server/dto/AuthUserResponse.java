package com.tingjian.server.dto;

import java.time.LocalDateTime;

public record AuthUserResponse(
        String id,
        String email,
        String displayName,
        LocalDateTime createdAt) {
}
