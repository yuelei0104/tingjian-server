package com.tingjian.server.dto;

public record ActiveSessionResponse(
        boolean available,
        SessionResponse session) {
}
