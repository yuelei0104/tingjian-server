package com.tingjian.server.common;

import java.time.Instant;

public record ApiResponse<T>(
        String requestId,
        String code,
        String message,
        T data,
        Instant timestamp) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(RequestTrace.currentOrCreate(), ErrorCode.OK.name(), "success", data, Instant.now());
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(RequestTrace.currentOrCreate(), errorCode.name(), message, null, Instant.now());
    }
}
