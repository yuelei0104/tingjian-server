package com.tingjian.server.dto;

import java.time.LocalDateTime;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        LocalDateTime accessExpiresAt,
        LocalDateTime refreshExpiresAt,
        AuthUserResponse user) {
}
