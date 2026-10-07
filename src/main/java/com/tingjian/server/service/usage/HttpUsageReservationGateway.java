package com.tingjian.server.service.usage;

import com.tingjian.server.common.BusinessException;
import com.tingjian.server.common.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
@ConditionalOnProperty(name = "tingjian.microservices.usage.enabled", havingValue = "true")
public class HttpUsageReservationGateway implements UsageReservationGateway {
    static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";
    private static final Logger log = LoggerFactory.getLogger(HttpUsageReservationGateway.class);
    private static final ParameterizedTypeReference<ApiEnvelope<ReservationResponse>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient client;
    private final boolean failOpen;

    public HttpUsageReservationGateway(
            @Value("${tingjian.microservices.usage.base-url:http://127.0.0.1:8093}") String baseUrl,
            @Value("${tingjian.microservices.usage.connect-timeout:1s}") Duration connectTimeout,
            @Value("${tingjian.microservices.usage.read-timeout:2s}") Duration readTimeout,
            @Value("${tingjian.microservices.usage.internal-token:}") String internalToken,
            @Value("${tingjian.microservices.usage.fail-open:true}") boolean failOpen) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        RestClient.Builder clientBuilder = RestClient.builder()
                .baseUrl(stripTrailingSlash(baseUrl))
                .requestFactory(requestFactory);
        if (internalToken != null && !internalToken.isBlank()) {
            clientBuilder.defaultHeader(INTERNAL_TOKEN_HEADER, internalToken.strip());
        }
        this.client = clientBuilder.build();
        this.failOpen = failOpen;
    }

    @Override
    public Reservation reserve(String userId, Metric metric, long amount, String idempotencyKey) {
        try {
            ApiEnvelope<ReservationResponse> envelope = client.post()
                    .uri("/internal/usage/reservations")
                    .body(new ReservationRequest(userId, metric, amount, idempotencyKey))
                    .retrieve()
                    .body(RESPONSE_TYPE);
            if (envelope == null || !envelope.success() || envelope.data() == null) {
                throw new IllegalStateException("usage service returned an invalid response");
            }
            ReservationResponse response = envelope.data();
            if (response.status() == ReservationStatus.REJECTED) {
                throw new BusinessException(ErrorCode.USAGE_QUOTA_EXCEEDED);
            }
            if ((response.status() != ReservationStatus.RESERVED
                    && response.status() != ReservationStatus.COMMITTED)
                    || response.reservationId() == null || response.reservationId().isBlank()) {
                throw new IllegalStateException("usage reservation was not created");
            }
            return new Reservation(response.reservationId(), true);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            if (!failOpen) {
                throw new BusinessException(ErrorCode.USAGE_SERVICE_UNAVAILABLE);
            }
            log.warn("Usage service unavailable; allowing an unmetered request");
            return Reservation.unmetered();
        }
    }

    @Override
    public void commit(Reservation reservation) {
        if (reservation == null || !reservation.metered()) return;
        try {
            client.post()
                    .uri("/internal/usage/reservations/{id}/commit", reservation.id())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException exception) {
            log.warn("Unable to commit usage reservation {}", reservation.id());
        }
    }

    @Override
    public void release(Reservation reservation) {
        if (reservation == null || !reservation.metered()) return;
        try {
            client.delete()
                    .uri("/internal/usage/reservations/{id}", reservation.id())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException exception) {
            log.warn("Unable to release usage reservation {}", reservation.id());
        }
    }

    private static String stripTrailingSlash(String value) {
        String result = value.strip();
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }

    private record ReservationRequest(
            String userId,
            Metric metric,
            long amount,
            String idempotencyKey) {
    }

    private record ApiEnvelope<T>(boolean success, T data, ApiError error, String requestId) {
    }

    private record ApiError(String code, String message) {
    }

    private record ReservationResponse(
            String reservationId,
            ReservationStatus status,
            Metric metric,
            long amount,
            long remaining,
            String resetDate) {
    }

    private enum ReservationStatus {
        RESERVED,
        COMMITTED,
        RELEASED,
        REJECTED
    }
}
