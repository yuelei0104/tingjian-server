package com.tingjian.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(
        name = "tingjian.auth.verification-delivery",
        havingValue = "log",
        matchIfMissing = true)
public class LoggingSecurityNotificationGateway implements SecurityNotificationGateway {
    private static final Logger log =
            LoggerFactory.getLogger(LoggingSecurityNotificationGateway.class);

    @Override
    public void newLogin(
            String email,
            String deviceName,
            String platform,
            String ipAddress,
            LocalDateTime occurredAt) {
        log.info("New login: account={}, device={}, platform={}, ip={}, occurredAt={}",
                maskEmail(email), deviceName, platform, ipAddress, occurredAt);
    }

    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        return at <= 1 ? "***" : email.charAt(0) + "***" + email.substring(at);
    }
}
