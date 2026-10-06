package com.tingjian.server.common;

import java.util.UUID;

public final class RequestTrace {
    public static final String HEADER_NAME = "X-Request-Id";
    public static final String MDC_KEY = "requestId";
    private static final int MAX_LENGTH = 64;
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private RequestTrace() {
    }

    public static String resolve(String candidate) {
        if (candidate != null) {
            String value = candidate.strip();
            if (value.length() >= 8 && value.length() <= MAX_LENGTH
                    && value.matches("[A-Za-z0-9._-]+")) {
                return value;
            }
        }
        return UUID.randomUUID().toString();
    }

    public static void set(String requestId) {
        CURRENT.set(requestId);
    }

    public static String currentOrCreate() {
        String value = CURRENT.get();
        return value == null ? UUID.randomUUID().toString() : value;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
