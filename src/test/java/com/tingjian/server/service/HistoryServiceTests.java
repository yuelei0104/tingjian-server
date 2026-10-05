package com.tingjian.server.service;

import com.tingjian.server.dao.HistoryDao;
import com.tingjian.server.dao.ConversationInsightDao;
import com.tingjian.server.entity.HistorySummaryEntity;
import com.tingjian.server.dto.SessionMessageResponse;
import com.tingjian.server.dto.SessionResponse;
import com.tingjian.server.service.insight.ConversationInsightResult;
import com.tingjian.server.service.insight.ConversationIntelligenceProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HistoryServiceTests {
    private final HistoryDao historyDao = mock(HistoryDao.class);
    private final SessionService sessionService = mock(SessionService.class);
    private final ConversationInsightDao insightDao = mock(ConversationInsightDao.class);
    private final ConversationIntelligenceProvider provider = mock(ConversationIntelligenceProvider.class);
    private final HistoryService service = new HistoryService(
            historyDao, sessionService, insightDao, provider);

    @Test
    void searchReturnsPaginationMetadata() {
        var row = new HistorySummaryEntity("id", "demo", "ENDED", null, null, 2, "hello");
        when(historyDao.count("owner", "hello")).thenReturn(1L);
        when(historyDao.search("owner", "hello", 20, 0)).thenReturn(List.of(row));

        var response = service.search("owner", "hello", 0, 20);

        assertEquals(1, response.total());
        assertEquals(1, response.items().size());
        assertEquals("hello", response.items().getFirst().preview());
        assertFalse(response.hasNext());
    }

    @Test
    void deleteMissingConversationIsIdempotent() {
        when(historyDao.existsForUpdate("id", "owner")).thenReturn(false);

        service.delete("owner", "id");

        verify(historyDao, never()).deleteMessages("id");
        verify(historyDao, never()).deleteConversation("id", "owner");
    }

    @Test
    void deleteRemovesMessagesBeforeConversation() {
        when(historyDao.existsForUpdate("id", "owner")).thenReturn(true);

        service.delete("owner", "id");

        verify(historyDao).deleteMessages("id");
        verify(historyDao).deleteConversation("id", "owner");
    }

    @Test
    void summarizeBuildsStableExtractiveSummary() {
        when(sessionService.get("owner", "id")).thenReturn(
                new SessionResponse("id", "demo", "ENDED", null, null));
        when(sessionService.messages("owner", "id")).thenReturn(List.of(
                new SessionMessageResponse("1", "client-1", 1, "OTHER", "第一句话", null),
                new SessionMessageResponse("2", "client-2", 2, "SELF", "第二句话", null)));
        when(insightDao.find("id", "owner")).thenReturn(Optional.empty());
        when(provider.analyze(org.mockito.ArgumentMatchers.any())).thenReturn(
                new ConversationInsightResult(
                        "会话摘要", List.of("第一句话"), List.of("第二句话"),
                        List.of("测试"), "平稳", "TEST_PROVIDER"));

        var response = service.summarize("owner", "id");

        assertEquals(2, response.messageCount());
        assertEquals("TEST_PROVIDER", response.generatedBy());
        assertEquals("会话摘要", response.summary());
        assertEquals(List.of("第一句话"), response.highlights());
        assertEquals(List.of("第二句话"), response.actionItems());
        verify(insightDao).save(org.mockito.ArgumentMatchers.any());
    }
}
