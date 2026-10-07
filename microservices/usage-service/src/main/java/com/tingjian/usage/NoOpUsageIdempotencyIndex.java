package com.tingjian.usage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnProperty(
        prefix = "tingjian.usage.redis-cache",
        name = "enabled",
        havingValue = "false")
class NoOpUsageIdempotencyIndex implements UsageIdempotencyIndex {
    @Override
    public Optional<String> findReservationId(String userId, String idempotencyKey) {
        return Optional.empty();
    }

    @Override
    public void remember(String userId, String idempotencyKey, String reservationId) {
        // MySQL unique constraints remain the source of truth when Redis is disabled.
    }
}
