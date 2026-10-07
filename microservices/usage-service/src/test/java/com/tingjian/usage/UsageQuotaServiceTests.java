package com.tingjian.usage;

import com.tingjian.contract.PlanTier;
import com.tingjian.contract.UsageMetric;
import com.tingjian.contract.UsageReservationRequest;
import com.tingjian.contract.UsageReservationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:usage;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.flyway.enabled=false",
                "spring.sql.init.mode=always",
                "spring.sql.init.schema-locations=classpath:schema-test.sql",
                "tingjian.usage.redis-cache.enabled=false",
                "tingjian.usage.expiry-scan-delay-ms=3600000"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UsageQuotaServiceTests {
    @Autowired
    private UsageQuotaService service;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void reservesCommitsAndPersistsUsage() {
        UsageReservationResponse reserved = service.reserve(request("user-1", 60, "audio-1"));
        UsageReservationResponse committed = service.commit(reserved.reservationId());

        assertThat(committed.status())
                .isEqualTo(UsageReservationResponse.ReservationStatus.COMMITTED);
        assertThat(service.summary("user-1").metrics()
                .get(UsageMetric.ASR_SECONDS).used()).isEqualTo(60);
        assertThat(jdbc.queryForObject(
                "SELECT used_amount FROM usage_daily_bucket WHERE user_id=? "
                        + "AND metric_code=?", Long.class, "user-1", "ASR_SECONDS"))
                .isEqualTo(60L);
    }

    @Test
    void repeatsSameReservationIdempotentlyAndRejectsConflicts() {
        UsageReservationResponse first = service.reserve(request("user-1", 60, "audio-1"));
        UsageReservationResponse second = service.reserve(request("user-1", 60, "audio-1"));

        assertThat(second.reservationId()).isEqualTo(first.reservationId());
        assertThat(service.summary("user-1").metrics()
                .get(UsageMetric.ASR_SECONDS).reserved()).isEqualTo(60);
        assertThatThrownBy(() -> service.reserve(request("user-1", 61, "audio-1")))
                .isInstanceOf(UsageException.class)
                .hasMessageContaining("幂等键");
    }

    @Test
    void rejectsRequestsBeyondPlanLimitAndPersistsPlan() {
        UsageReservationResponse rejected = service.reserve(
                request("user-1", 1_801, "audio-too-long"));
        service.setPlan("user-1", PlanTier.PRO);

        assertThat(rejected.status())
                .isEqualTo(UsageReservationResponse.ReservationStatus.REJECTED);
        assertThat(service.summary("user-1").plan()).isEqualTo(PlanTier.PRO);
        assertThat(jdbc.queryForObject(
                "SELECT plan_code FROM usage_user_plan WHERE user_id=?",
                String.class, "user-1")).isEqualTo("PRO");
    }

    @Test
    void releasesExpiredReservationsAndRestoresRemainingQuota() {
        UsageReservationResponse reserved = service.reserve(request("user-1", 60, "audio-1"));
        jdbc.update("UPDATE usage_reservation SET expires_at=? WHERE id=?",
                Timestamp.from(Instant.EPOCH), reserved.reservationId());

        int released = service.releaseExpiredReservations();

        assertThat(released).isEqualTo(1);
        assertThat(service.summary("user-1").metrics()
                .get(UsageMetric.ASR_SECONDS).reserved()).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT status FROM usage_reservation WHERE id=?",
                String.class, reserved.reservationId())).isEqualTo("RELEASED");
    }

    @Test
    void idempotentRetryCannotReviveAnExpiredReservation() {
        UsageReservationResponse reserved = service.reserve(request("user-1", 60, "audio-1"));
        jdbc.update("UPDATE usage_reservation SET expires_at=? WHERE id=?",
                Timestamp.from(Instant.EPOCH), reserved.reservationId());

        UsageReservationResponse retried = service.reserve(request("user-1", 60, "audio-1"));

        assertThat(retried.reservationId()).isEqualTo(reserved.reservationId());
        assertThat(retried.status())
                .isEqualTo(UsageReservationResponse.ReservationStatus.RELEASED);
        assertThat(service.summary("user-1").metrics()
                .get(UsageMetric.ASR_SECONDS).reserved()).isZero();
    }

    private UsageReservationRequest request(String userId, long amount, String key) {
        return new UsageReservationRequest(userId, UsageMetric.ASR_SECONDS, amount, key);
    }
}
