package com.tingjian.server.service;

import java.time.LocalDateTime;

public interface SecurityNotificationGateway {
    void newLogin(
            String email,
            String deviceName,
            String platform,
            String ipAddress,
            LocalDateTime occurredAt);
}
