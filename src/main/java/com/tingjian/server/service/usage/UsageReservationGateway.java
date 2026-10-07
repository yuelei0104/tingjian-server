package com.tingjian.server.service.usage;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

public interface UsageReservationGateway {
    Reservation reserve(String userId, Metric metric, long amount, String idempotencyKey);

    void commit(Reservation reservation);

    void release(Reservation reservation);

    Optional<UsageSnapshot> summary(String userId);

    enum Metric {
        ASR_SECONDS,
        AI_REQUESTS,
        TTS_CHARACTERS
    }

    record Reservation(String id, boolean metered) {
        public static Reservation unmetered() {
            return new Reservation("", false);
        }
    }

    record UsageSnapshot(
            String planCode,
            LocalDate resetDate,
            Map<Metric, MetricUsage> metrics) {
    }

    record MetricUsage(long used, long reserved, long limit, long remaining) {
    }
}
