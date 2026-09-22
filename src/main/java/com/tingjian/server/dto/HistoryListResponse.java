package com.tingjian.server.dto;

import java.util.List;

public record HistoryListResponse(
        List<HistoryItemResponse> items,
        int page,
        int size,
        long total,
        boolean hasNext) {
}
