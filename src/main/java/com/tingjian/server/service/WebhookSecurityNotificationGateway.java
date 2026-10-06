package com.tingjian.server.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(name = "tingjian.auth.verification-delivery", havingValue = "webhook")
public class WebhookSecurityNotificationGateway implements SecurityNotificationGateway {
    private final RestClient client;
    private final String token;

    public WebhookSecurityNotificationGateway(
            @Value("${tingjian.auth.verification-webhook-url}") String url,
            @Value("${tingjian.auth.verification-webhook-token:}") String token) {
        this.client = RestClient.builder().baseUrl(url).build();
        this.token = token;
    }

    @Override
    public void newLogin(
            String email,
            String deviceName,
            String platform,
            String ipAddress,
            LocalDateTime occurredAt) {
        RestClient.RequestBodySpec request = client.post();
        if (!token.isBlank()) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        request.body(new EventRequest(
                "NEW_LOGIN", email, deviceName, platform, ipAddress, occurredAt))
                .retrieve().toBodilessEntity();
    }

    private record EventRequest(
            String event,
            String destination,
            String deviceName,
            String platform,
            String ipAddress,
            LocalDateTime occurredAt) {
    }
}
