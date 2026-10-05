package com.tingjian.server.service;

import com.tingjian.server.dao.AccessibilityPreferenceDao;
import com.tingjian.server.dto.AccessibilityPreferenceUpdateRequest;
import com.tingjian.server.entity.AccessibilityPreferenceEntity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccessibilityPreferenceServiceTests {
    private final AccessibilityPreferenceDao dao = mock(AccessibilityPreferenceDao.class);
    private final AccessibilityPreferenceService service =
            new AccessibilityPreferenceService(dao);

    @Test
    void getReturnsAccessibleDefaultsForNewUser() {
        when(dao.find("owner")).thenReturn(Optional.empty());

        var response = service.get("owner");

        assertFalse(response.configured());
        assertFalse(response.highContrast());
        assertTrue(response.visualAlerts());
        assertFalse(response.systemNotifications());
        assertTrue(response.captionFollow());
    }

    @Test
    void updateUpsertsEveryAccessibilitySetting() {
        var request = new AccessibilityPreferenceUpdateRequest(
                true, false, false, true, false);

        var response = service.update("owner", request);

        var captor = ArgumentCaptor.forClass(AccessibilityPreferenceEntity.class);
        verify(dao).upsert(captor.capture());
        assertTrue(captor.getValue().highContrast());
        assertTrue(captor.getValue().strongVibration());
        assertFalse(captor.getValue().captionFollow());
        assertTrue(response.configured());
    }
}
