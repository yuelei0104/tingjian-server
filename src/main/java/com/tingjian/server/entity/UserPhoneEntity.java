package com.tingjian.server.entity;

import java.time.LocalDateTime;

public record UserPhoneEntity(
        String userId,
        String phone,
        LocalDateTime verifiedAt,
        LocalDateTime updatedAt) {
}
