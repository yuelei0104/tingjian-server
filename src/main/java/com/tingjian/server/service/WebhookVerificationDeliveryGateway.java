package com.tingjian.server.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "tingjian.auth.verification-delivery", havingValue = "webhook")
public class WebhookVerificationDeliveryGateway implements VerificationDeliveryGateway {
    private final RestClient client;
    private final String token;

    public WebhookVerificationDeliveryGateway(
            @Value("${tingjian.auth.verification-webhook-url}") String webhookUrl,
            @Value("${tingjian.auth.verification-webhook-token:}") String token) {
        this.client = RestClient.builder().baseUrl(webhookUrl).build();
        this.token = token;
    }

    @Override
    public void send(
            String channel, String destination, String purpose, String code, int validMinutes) {
        RestClient.RequestBodySpec request = client.post();
        if (!token.isBlank()) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        request.body(new DeliveryRequest(channel, destination, purpose, code, validMinutes))
                .retrieve()
                .toBodilessEntity();
    }

    private record DeliveryRequest(
            String channel,
            String destination,
            String purpose,
            String code,
            int validMinutes) {
    }
}
