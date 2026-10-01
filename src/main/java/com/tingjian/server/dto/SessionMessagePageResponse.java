package com.tingjian.server.dto;

import java.util.List;

public record SessionMessagePageResponse(
        List<SessionMessageResponse> items,
        long nextAfterSequence,
        boolean hasNext) {
}
