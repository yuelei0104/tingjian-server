package com.tingjian.server.service;

import com.tingjian.server.common.BusinessException;
import com.tingjian.server.dao.HistoryDao;
import com.tingjian.server.entity.HistorySummaryEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HistoryServiceTests {
    private final HistoryDao historyDao = mock(HistoryDao.class);
    private final SessionService sessionService = mock(SessionService.class);
    private final HistoryService service = new HistoryService(historyDao, sessionService);

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
    void deleteRejectsAnotherOwnersConversation() {
        when(historyDao.existsForUpdate("id", "owner")).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.delete("owner", "id"));

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
}
