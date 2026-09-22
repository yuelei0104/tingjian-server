package com.tingjian.server.common;

import java.time.Instant;
import java.util.UUID;

public record ApiResponse<T>(
        String requestId,
        String code,
        String message,
        T data,
        Instant timestamp) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(requestId(), ErrorCode.OK.name(), "success", data, Instant.now());
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(requestId(), errorCode.name(), message, null, Instant.now());
    }

    private static String requestId() {
        return UUID.randomUUID().toString();
    }
}
