package com.tingjian.server.service;

import com.tingjian.server.common.BusinessException;
import com.tingjian.server.dao.KeywordDao;
import com.tingjian.server.dto.KeywordUpsertRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KeywordServiceTests {
    private final KeywordDao keywordDao = mock(KeywordDao.class);
    private final KeywordService service = new KeywordService(keywordDao);

    @Test
    void createNormalizesPhrase() {
        var response = service.create(
                "owner", new KeywordUpsertRequest("  老师  ", true, 80, true));

        assertEquals("老师", response.phrase());
        assertEquals(80, response.priority());
        verify(keywordDao).create(any());
    }

    @Test
    void deleteRejectsMissingKeyword() {
        when(keywordDao.delete("id", "owner")).thenReturn(0);

        assertThrows(BusinessException.class, () -> service.delete("owner", "id"));
    }
}
