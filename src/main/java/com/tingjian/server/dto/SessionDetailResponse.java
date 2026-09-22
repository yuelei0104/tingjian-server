package com.tingjian.server.dto;

import java.util.List;

public record SessionDetailResponse(
        SessionResponse session,
        List<SessionMessageResponse> messages) {
}
