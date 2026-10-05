package com.tingjian.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "tingjian.auth.verification-delivery",
        havingValue = "log",
        matchIfMissing = true)
public class LoggingVerificationDeliveryGateway implements VerificationDeliveryGateway {
    private static final Logger log = LoggerFactory.getLogger(LoggingVerificationDeliveryGateway.class);

    @Override
    public void send(
            String channel, String destination, String purpose, String code, int validMinutes) {
        log.warn("Development verification code: channel={}, destination={}, purpose={}, "
                        + "code={}, validMinutes={}",
                channel, mask(destination), purpose, code, validMinutes);
    }

    private static String mask(String destination) {
        int at = destination.indexOf('@');
        if (at > 1) {
            return destination.charAt(0) + "***" + destination.substring(at);
        }
        return destination.length() <= 4
                ? "****"
                : "***" + destination.substring(destination.length() - 4);
    }
}
