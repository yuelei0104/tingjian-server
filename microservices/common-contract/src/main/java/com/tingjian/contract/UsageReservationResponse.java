package com.tingjian.contract;

import java.time.LocalDate;

public record UsageReservationResponse(
        String reservationId,
        ReservationStatus status,
        UsageMetric metric,
        long amount,
        long remaining,
        LocalDate resetDate
) {
    public enum ReservationStatus {
        RESERVED,
        COMMITTED,
        RELEASED,
        REJECTED
    }
}
