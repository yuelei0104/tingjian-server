package com.tingjian.server.dto;

import java.util.List;

public record HomeResponse(
        HomeOverviewResponse overview,
        List<HistoryItemResponse> recentConversations,
        List<HomeSceneResponse> scenes,
        HomePlanResponse plan) {
}
