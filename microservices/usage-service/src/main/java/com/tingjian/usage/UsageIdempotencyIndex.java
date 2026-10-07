package com.tingjian.usage;

import java.util.Optional;

interface UsageIdempotencyIndex {
    Optional<String> findReservationId(String userId, String idempotencyKey);

    void remember(String userId, String idempotencyKey, String reservationId);
}
