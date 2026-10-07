package com.tingjian.server.service.usage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnProperty(
        name = "tingjian.microservices.usage.enabled",
        havingValue = "false",
        matchIfMissing = true)
public class NoOpUsageReservationGateway implements UsageReservationGateway {
    @Override
    public Reservation reserve(String userId, Metric metric, long amount, String idempotencyKey) {
        return Reservation.unmetered();
    }

    @Override
    public void commit(Reservation reservation) {
        // Metering is disabled for local development.
    }

    @Override
    public void release(Reservation reservation) {
        // Metering is disabled for local development.
    }

    @Override
    public Optional<UsageSnapshot> summary(String userId) {
        return Optional.empty();
    }
}
