package com.tingjian.server.service;

import com.tingjian.server.dao.UserPreferenceDao;
import com.tingjian.server.dto.UserPreferenceUpdateRequest;
import com.tingjian.server.entity.UserPreferenceEntity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserPreferenceServiceTests {
    private final UserPreferenceDao dao = mock(UserPreferenceDao.class);
    private final UserPreferenceService service = new UserPreferenceService(dao);

    @Test
    void getReturnsUnconfiguredDefaultsForNewUser() {
        when(dao.find("owner")).thenReturn(Optional.empty());

        var response = service.get("owner");

        assertFalse(response.configured());
        assertEquals("中英混合", response.recognitionLanguage());
        assertEquals(1.0, response.ttsSpeed());
        assertTrue(response.keywordVibration());
    }

    @Test
    void updateUpsertsAllSettings() {
        var request = new UserPreferenceUpdateRequest(
                true, "中文", "清晰", 1.2, "中文", false, true, true);

        var response = service.update("owner", request);

        var captor = ArgumentCaptor.forClass(UserPreferenceEntity.class);
        verify(dao).upsert(captor.capture());
        assertEquals("owner", captor.getValue().ownerId());
        assertEquals("清晰", captor.getValue().voiceStyle());
        assertTrue(response.configured());
        assertTrue(response.autoSummary());
    }
}
