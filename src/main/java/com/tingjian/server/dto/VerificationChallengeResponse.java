package com.tingjian.server.dto;

import java.time.LocalDateTime;

public record VerificationChallengeResponse(
        String verificationId,
        LocalDateTime expiresAt) {
}
