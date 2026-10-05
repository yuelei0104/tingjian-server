package com.tingjian.server.dto;

import java.time.LocalDateTime;

public record AccessibilityPreferenceResponse(
        boolean configured,
        boolean highContrast,
        boolean visualAlerts,
        boolean systemNotifications,
        boolean strongVibration,
        boolean captionFollow,
        LocalDateTime updatedAt) {
}
