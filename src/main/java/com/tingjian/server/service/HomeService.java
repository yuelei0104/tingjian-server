package com.tingjian.server.service;

import com.tingjian.server.dao.HistoryDao;
import com.tingjian.server.dao.HomeDao;
import com.tingjian.server.dto.HistoryItemResponse;
import com.tingjian.server.dto.HomeOverviewResponse;
import com.tingjian.server.dto.HomePlanResponse;
import com.tingjian.server.dto.HomeResponse;
import com.tingjian.server.dto.HomeSceneResponse;
import com.tingjian.server.entity.HistorySummaryEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HomeService {
    private static final List<HomeSceneResponse> SCENES = List.of(
            new HomeSceneResponse("CLASSROOM", "课堂"),
            new HomeSceneResponse("MEETING", "会议"),
            new HomeSceneResponse("MEDICAL", "就医"),
            new HomeSceneResponse("DAILY", "日常"));

    private static final HomePlanResponse V1_PLAN = new HomePlanResponse(
            "V1_BETA", "V1 内测演示", "当前版本暂未开放购买", false);

    private final HomeDao homeDao;
    private final HistoryDao historyDao;

    public HomeService(HomeDao homeDao, HistoryDao historyDao) {
        this.homeDao = homeDao;
        this.historyDao = historyDao;
    }

    public HomeResponse get(String ownerId, int recentSize) {
        var overview = homeDao.overview(ownerId);
        var recent = historyDao.search(ownerId, null, recentSize, 0).stream()
                .map(HomeService::toHistoryItem)
                .toList();

        return new HomeResponse(
                new HomeOverviewResponse(
                        overview.conversationCount(),
                        overview.messageCount(),
                        overview.totalDurationSeconds()),
                recent,
                SCENES,
                V1_PLAN);
    }

    private static HistoryItemResponse toHistoryItem(HistorySummaryEntity entity) {
        return new HistoryItemResponse(
                entity.id(), entity.title(), entity.status(), entity.startedAt(), entity.endedAt(),
                entity.messageCount(), entity.preview());
    }
}
