package com.tingjian.server.entity;

import java.time.LocalDateTime;

public record UserEntity(
        String id,
        String email,
        String passwordHash,
        String displayName,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
