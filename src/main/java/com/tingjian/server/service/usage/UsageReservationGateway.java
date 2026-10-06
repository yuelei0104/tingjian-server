package com.tingjian.server.service.usage;

public interface UsageReservationGateway {
    Reservation reserve(String userId, Metric metric, long amount, String idempotencyKey);

    void commit(Reservation reservation);

    void release(Reservation reservation);

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
}
