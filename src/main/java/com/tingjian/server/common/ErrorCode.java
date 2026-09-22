package com.tingjian.server.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    OK(HttpStatus.OK, "success"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "请求参数不正确"),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "会话不存在"),
    SESSION_ENDED(HttpStatus.CONFLICT, "会话已结束"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
