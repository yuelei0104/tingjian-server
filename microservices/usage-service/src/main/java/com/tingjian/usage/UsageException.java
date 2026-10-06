package com.tingjian.usage;

class UsageException extends RuntimeException {
    private final String code;

    UsageException(String code, String message) {
        super(message);
        this.code = code;
    }

    String code() {
        return code;
    }
}
