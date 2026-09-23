package com.tingjian.server.service;

import com.tingjian.server.common.BusinessException;
import com.tingjian.server.dao.QuickPhraseDao;
import com.tingjian.server.dto.QuickPhraseUpsertRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuickPhraseServiceTests {
    private final QuickPhraseDao quickPhraseDao = mock(QuickPhraseDao.class);
    private final QuickPhraseService service = new QuickPhraseService(quickPhraseDao);

    @Test
    void createNormalizesContentAndCategory() {
        var response = service.create(
                "owner", new QuickPhraseUpsertRequest("  请再说一次  ", "  日常  ", 10, true));

        assertEquals("请再说一次", response.content());
        assertEquals("日常", response.category());
        verify(quickPhraseDao).create(any());
    }

    @Test
    void deleteRejectsMissingPhrase() {
        when(quickPhraseDao.delete("id", "owner")).thenReturn(0);

        assertThrows(BusinessException.class, () -> service.delete("owner", "id"));
    }
}
