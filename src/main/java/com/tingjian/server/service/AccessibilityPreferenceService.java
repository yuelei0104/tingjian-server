package com.tingjian.server.service;

import com.tingjian.server.dao.AccessibilityPreferenceDao;
import com.tingjian.server.dto.AccessibilityPreferenceResponse;
import com.tingjian.server.dto.AccessibilityPreferenceUpdateRequest;
import com.tingjian.server.entity.AccessibilityPreferenceEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class AccessibilityPreferenceService {
    private final AccessibilityPreferenceDao accessibilityPreferenceDao;

    public AccessibilityPreferenceService(
            AccessibilityPreferenceDao accessibilityPreferenceDao) {
        this.accessibilityPreferenceDao = accessibilityPreferenceDao;
    }

    public AccessibilityPreferenceResponse get(String ownerId) {
        return accessibilityPreferenceDao.find(ownerId)
                .map(entity -> toResponse(entity, true))
                .orElseGet(AccessibilityPreferenceService::defaults);
    }

    public AccessibilityPreferenceResponse update(
            String ownerId, AccessibilityPreferenceUpdateRequest request) {
        AccessibilityPreferenceEntity entity = new AccessibilityPreferenceEntity(
                ownerId, request.highContrast(), request.visualAlerts(),
                request.systemNotifications(), request.strongVibration(),
                request.captionFollow(), LocalDateTime.now(ZoneOffset.UTC));
        accessibilityPreferenceDao.upsert(entity);
        return toResponse(entity, true);
    }

    private static AccessibilityPreferenceResponse defaults() {
        return new AccessibilityPreferenceResponse(
                false, false, true, false, false, true, null);
    }

    private static AccessibilityPreferenceResponse toResponse(
            AccessibilityPreferenceEntity entity, boolean configured) {
        return new AccessibilityPreferenceResponse(
                configured, entity.highContrast(), entity.visualAlerts(),
                entity.systemNotifications(), entity.strongVibration(),
                entity.captionFollow(), entity.updatedAt());
    }
}
