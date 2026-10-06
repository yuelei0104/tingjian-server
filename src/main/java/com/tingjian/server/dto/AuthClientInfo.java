package com.tingjian.server.dto;

public record AuthClientInfo(
        String deviceId,
        String deviceName,
        String platform,
        String appVersion,
        String ipAddress) {
}
