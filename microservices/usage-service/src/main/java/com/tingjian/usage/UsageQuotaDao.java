package com.tingjian.usage;

import com.tingjian.contract.PlanTier;
import com.tingjian.contract.UsageMetric;
import com.tingjian.contract.UsageReservationResponse.ReservationStatus;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
class UsageQuotaDao {
    private final JdbcTemplate jdbc;

    UsageQuotaDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    PlanTier lockPlan(String userId) {
        try {
            jdbc.update("INSERT INTO usage_user_plan(user_id, plan_code) VALUES (?, ?)",
                    userId, PlanTier.FREE.name());
        } catch (DuplicateKeyException ignored) {
            // Another request or an earlier day already initialized the user.
        }
        return jdbc.queryForObject(
                "SELECT plan_code FROM usage_user_plan WHERE user_id=? FOR UPDATE",
                (rs, row) -> PlanTier.valueOf(rs.getString(1)), userId);
    }

    void updatePlan(String userId, PlanTier plan) {
        jdbc.update("UPDATE usage_user_plan SET plan_code=? WHERE user_id=?", plan.name(), userId);
    }

    Bucket lockBucket(String userId, LocalDate date, UsageMetric metric) {
        try {
            jdbc.update("INSERT INTO usage_daily_bucket"
                            + "(user_id, usage_date, metric_code, used_amount, reserved_amount) "
                            + "VALUES (?, ?, ?, 0, 0)",
                    userId, date, metric.name());
        } catch (DuplicateKeyException ignored) {
            // The bucket already exists.
        }
        return jdbc.queryForObject(
                "SELECT used_amount, reserved_amount FROM usage_daily_bucket "
                        + "WHERE user_id=? AND usage_date=? AND metric_code=? FOR UPDATE",
                (rs, row) -> new Bucket(rs.getLong(1), rs.getLong(2)),
                userId, date, metric.name());
    }

    void updateBucket(String userId, LocalDate date, UsageMetric metric, Bucket bucket) {
        jdbc.update("UPDATE usage_daily_bucket SET used_amount=?, reserved_amount=? "
                        + "WHERE user_id=? AND usage_date=? AND metric_code=?",
                bucket.used(), bucket.reserved(), userId, date, metric.name());
    }

    Optional<ReservationRecord> findByIdempotency(String userId, String idempotencyKey) {
        return jdbc.query("SELECT id, user_id, usage_date, metric_code, amount, "
                        + "idempotency_key, status, expires_at FROM usage_reservation "
                        + "WHERE user_id=? AND idempotency_key=?",
                this::mapReservation, userId, idempotencyKey).stream().findFirst();
    }

    Optional<ReservationRecord> findById(String id) {
        return jdbc.query("SELECT id, user_id, usage_date, metric_code, amount, "
                        + "idempotency_key, status, expires_at FROM usage_reservation WHERE id=?",
                this::mapReservation, id).stream().findFirst();
    }

    ReservationRecord lockReservation(String id) {
        return jdbc.query("SELECT id, user_id, usage_date, metric_code, amount, "
                        + "idempotency_key, status, expires_at FROM usage_reservation "
                        + "WHERE id=? FOR UPDATE",
                this::mapReservation, id).stream().findFirst()
                .orElseThrow(() -> new UsageException(
                        "RESERVATION_NOT_FOUND", "用量预占记录不存在"));
    }

    void insertReservation(ReservationRecord reservation) {
        jdbc.update("INSERT INTO usage_reservation(id, user_id, usage_date, metric_code, amount, "
                        + "idempotency_key, status, expires_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                reservation.id(), reservation.userId(), reservation.day(),
                reservation.metric().name(), reservation.amount(), reservation.idempotencyKey(),
                reservation.status().name(), Timestamp.from(reservation.expiresAt()));
    }

    void updateReservationStatus(String id, ReservationStatus status) {
        jdbc.update("UPDATE usage_reservation SET status=? WHERE id=?", status.name(), id);
    }

    Map<UsageMetric, Bucket> buckets(String userId, LocalDate date) {
        EnumMap<UsageMetric, Bucket> result = new EnumMap<>(UsageMetric.class);
        jdbc.query("SELECT metric_code, used_amount, reserved_amount FROM usage_daily_bucket "
                        + "WHERE user_id=? AND usage_date=?",
                (RowCallbackHandler) rs -> result.put(
                        UsageMetric.valueOf(rs.getString(1)),
                        new Bucket(rs.getLong(2), rs.getLong(3))),
                userId, date);
        return result;
    }

    List<UserDay> expiredUserDays(Instant now, int limit) {
        return jdbc.query("SELECT DISTINCT user_id, usage_date FROM usage_reservation "
                        + "WHERE status=? AND expires_at<=? ORDER BY usage_date LIMIT ?",
                (rs, row) -> new UserDay(
                        rs.getString(1), rs.getObject(2, LocalDate.class)),
                ReservationStatus.RESERVED.name(), Timestamp.from(now), limit);
    }

    int releaseExpired(String userId, LocalDate date, Instant now) {
        List<ReservationRecord> expired = jdbc.query(
                "SELECT id, user_id, usage_date, metric_code, amount, idempotency_key, status, "
                        + "expires_at FROM usage_reservation WHERE user_id=? AND usage_date=? "
                        + "AND status=? AND expires_at<=? FOR UPDATE",
                this::mapReservation, userId, date,
                ReservationStatus.RESERVED.name(), Timestamp.from(now));
        if (expired.isEmpty()) return 0;

        EnumMap<UsageMetric, Long> totals = new EnumMap<>(UsageMetric.class);
        for (ReservationRecord reservation : expired) {
            totals.merge(reservation.metric(), reservation.amount(), Long::sum);
        }
        totals.forEach((metric, amount) -> {
            Bucket bucket = lockBucket(userId, date, metric);
            updateBucket(userId, date, metric,
                    new Bucket(bucket.used(), Math.max(0, bucket.reserved() - amount)));
        });
        List<Object[]> updates = new ArrayList<>();
        for (ReservationRecord reservation : expired) {
            updates.add(new Object[]{ReservationStatus.RELEASED.name(), reservation.id()});
        }
        jdbc.batchUpdate("UPDATE usage_reservation SET status=? WHERE id=?", updates);
        return expired.size();
    }

    private ReservationRecord mapReservation(java.sql.ResultSet rs, int row)
            throws java.sql.SQLException {
        return new ReservationRecord(
                rs.getString("id"),
                rs.getString("user_id"),
                rs.getObject("usage_date", LocalDate.class),
                UsageMetric.valueOf(rs.getString("metric_code")),
                rs.getLong("amount"),
                rs.getString("idempotency_key"),
                ReservationStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("expires_at").toInstant());
    }

    record Bucket(long used, long reserved) {
    }

    record ReservationRecord(
            String id,
            String userId,
            LocalDate day,
            UsageMetric metric,
            long amount,
            String idempotencyKey,
            ReservationStatus status,
            Instant expiresAt) {
    }

    record UserDay(String userId, LocalDate day) {
    }
}
