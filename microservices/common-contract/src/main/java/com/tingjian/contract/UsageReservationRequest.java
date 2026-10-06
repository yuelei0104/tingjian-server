package com.tingjian.contract;

public record UsageReservationRequest(
        String userId,
        UsageMetric metric,
        long amount,
        String idempotencyKey
) {
}
