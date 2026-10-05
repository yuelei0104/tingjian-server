package com.tingjian.server.dto;

import jakarta.validation.constraints.NotNull;

public record AccessibilityPreferenceUpdateRequest(
        @NotNull Boolean highContrast,
        @NotNull Boolean visualAlerts,
        @NotNull Boolean systemNotifications,
        @NotNull Boolean strongVibration,
        @NotNull Boolean captionFollow) {
}
