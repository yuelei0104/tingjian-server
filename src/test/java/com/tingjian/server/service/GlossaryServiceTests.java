package com.tingjian.server.service;

import com.tingjian.server.common.BusinessException;
import com.tingjian.server.dao.GlossaryDao;
import com.tingjian.server.dto.GlossaryUpsertRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GlossaryServiceTests {
    private final GlossaryDao glossaryDao = mock(GlossaryDao.class);
    private final GlossaryService service = new GlossaryService(glossaryDao);

    @Test
    void createNormalizesTermAndEmptyAlias() {
        var response = service.create(
                "owner", new GlossaryUpsertRequest("  人工智能  ", "  ", "zh-CN", "课堂", 90, true));

        assertEquals("人工智能", response.term());
        assertNull(response.alias());
        verify(glossaryDao).create(any());
    }

    @Test
    void deleteRejectsMissingTerm() {
        when(glossaryDao.delete("id", "owner")).thenReturn(0);

        assertThrows(BusinessException.class, () -> service.delete("owner", "id"));
    }
}
