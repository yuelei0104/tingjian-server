package com.tingjian.usage;

import com.tingjian.contract.PlanTier;
import com.tingjian.contract.UsageMetric;
import com.tingjian.contract.UsageReservationRequest;
import com.tingjian.contract.UsageReservationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsageQuotaServiceTests {
    private UsageQuotaService service;

    @BeforeEach
    void setUp() {
        service = new UsageQuotaService(Clock.fixed(
                Instant.parse("2026-10-06T10:00:00Z"), UsageQuotaService.BUSINESS_ZONE));
    }

    @Test
    void reservesCommitsAndSummarizesUsage() {
        UsageReservationResponse reserved = service.reserve(request("user-1", 60, "audio-1"));
        UsageReservationResponse committed = service.commit(reserved.reservationId());

        assertThat(committed.status()).isEqualTo(UsageReservationResponse.ReservationStatus.COMMITTED);
        assertThat(service.summary("user-1").metrics().get(UsageMetric.ASR_SECONDS).used()).isEqualTo(60);
        assertThat(service.summary("user-1").metrics().get(UsageMetric.ASR_SECONDS).reserved()).isZero();
    }

    @Test
    void repeatsSameReservationIdempotently() {
        UsageReservationResponse first = service.reserve(request("user-1", 60, "audio-1"));
        UsageReservationResponse second = service.reserve(request("user-1", 60, "audio-1"));

        assertThat(second.reservationId()).isEqualTo(first.reservationId());
        assertThat(service.summary("user-1").metrics().get(UsageMetric.ASR_SECONDS).reserved()).isEqualTo(60);
    }

    @Test
    void rejectsRequestsBeyondPlanLimit() {
        UsageReservationResponse response = service.reserve(request("user-1", 1_801, "audio-too-long"));

        assertThat(response.status()).isEqualTo(UsageReservationResponse.ReservationStatus.REJECTED);
        assertThat(service.summary("user-1").metrics().get(UsageMetric.ASR_SECONDS).reserved()).isZero();
    }

    @Test
    void proPlanGetsHigherLimitAndConflictingIdempotencyFails() {
        service.setPlan("user-1", PlanTier.PRO);
        service.reserve(request("user-1", 2_000, "audio-1"));

        assertThat(service.summary("user-1").plan()).isEqualTo(PlanTier.PRO);
        assertThatThrownBy(() -> service.reserve(request("user-1", 2_001, "audio-1")))
                .isInstanceOf(UsageException.class)
                .hasMessageContaining("幂等键");
    }

    private UsageReservationRequest request(String userId, long amount, String key) {
        return new UsageReservationRequest(userId, UsageMetric.ASR_SECONDS, amount, key);
    }
}
