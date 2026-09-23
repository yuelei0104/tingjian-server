package com.tingjian.server.service;

import com.tingjian.server.dao.PrivacyDao;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrivacyServiceTests {
    private final PrivacyDao privacyDao = mock(PrivacyDao.class);
    private final PrivacyService service = new PrivacyService(privacyDao);

    @Test
    void deleteHistoryRemovesMessagesBeforeConversations() {
        when(privacyDao.deleteConversationMessages("owner")).thenReturn(5);
        when(privacyDao.deleteConversations("owner")).thenReturn(2);

        var response = service.deleteHistory("owner");

        InOrder order = inOrder(privacyDao);
        order.verify(privacyDao).deleteConversationMessages("owner");
        order.verify(privacyDao).deleteConversations("owner");
        assertEquals(5, response.messages());
        assertEquals(2, response.conversations());
    }

    @Test
    void deletePersonalizationReturnsDeletionCounts() {
        when(privacyDao.deleteKeywords("owner")).thenReturn(1);
        when(privacyDao.deleteGlossaryTerms("owner")).thenReturn(2);
        when(privacyDao.deleteQuickPhrases("owner")).thenReturn(3);

        var response = service.deletePersonalization("owner");

        assertEquals(1, response.keywords());
        assertEquals(2, response.glossaryTerms());
        assertEquals(3, response.quickPhrases());
    }
}
