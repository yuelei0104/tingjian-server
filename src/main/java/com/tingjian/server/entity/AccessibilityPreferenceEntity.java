package com.tingjian.server.entity;

import java.time.LocalDateTime;

public record AccessibilityPreferenceEntity(
        String ownerId,
        boolean highContrast,
        boolean visualAlerts,
        boolean systemNotifications,
        boolean strongVibration,
        boolean captionFollow,
        LocalDateTime updatedAt) {
}
