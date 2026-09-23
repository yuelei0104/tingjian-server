package com.tingjian.server.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    OK(HttpStatus.OK, "success"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "请求参数不正确"),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "会话不存在"),
    SESSION_ENDED(HttpStatus.CONFLICT, "会话已结束"),
    KEYWORD_NOT_FOUND(HttpStatus.NOT_FOUND, "关键词不存在"),
    GLOSSARY_NOT_FOUND(HttpStatus.NOT_FOUND, "术语不存在"),
    QUICK_PHRASE_NOT_FOUND(HttpStatus.NOT_FOUND, "常用语不存在"),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "数据已存在"),
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
