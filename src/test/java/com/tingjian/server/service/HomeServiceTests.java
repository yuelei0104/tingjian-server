package com.tingjian.server.service;

import com.tingjian.server.dao.HistoryDao;
import com.tingjian.server.dao.HomeDao;
import com.tingjian.server.entity.HistorySummaryEntity;
import com.tingjian.server.entity.HomeOverviewEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HomeServiceTests {
    private final HomeDao homeDao = mock(HomeDao.class);
    private final HistoryDao historyDao = mock(HistoryDao.class);
    private final HomeService service = new HomeService(homeDao, historyDao);

    @Test
    void getReturnsCompleteHomeData() {
        var startedAt = LocalDateTime.of(2026, 9, 23, 12, 0);
        var endedAt = startedAt.plusMinutes(12);
        var recent = new HistorySummaryEntity(
                "session-1", "和朋友聊周末", "ENDED", startedAt, endedAt, 8, "周六去公园吧");
        when(homeDao.overview("owner")).thenReturn(new HomeOverviewEntity(6, 42, 3_600));
        when(historyDao.search("owner", null, 3, 0)).thenReturn(List.of(recent));

        var response = service.get("owner", 3);

        assertEquals(6, response.overview().conversationCount());
        assertEquals(42, response.overview().messageCount());
        assertEquals(3_600, response.overview().totalDurationSeconds());
        assertEquals("和朋友聊周末", response.recentConversations().getFirst().title());
        assertEquals(List.of("课堂", "会议", "就医", "日常"),
                response.scenes().stream().map(scene -> scene.name()).toList());
        assertEquals("V1_BETA", response.plan().code());
        assertFalse(response.plan().purchasable());
    }

    @Test
    void getUsesRequestedRecentConversationLimit() {
        when(homeDao.overview("owner")).thenReturn(new HomeOverviewEntity(0, 0, 0));
        when(historyDao.search("owner", null, 5, 0)).thenReturn(List.of());

        service.get("owner", 5);

        verify(historyDao).search("owner", null, 5, 0);
    }
}
