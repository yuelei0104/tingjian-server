package com.tingjian.usage;

import com.tingjian.contract.PlanTier;
import com.tingjian.contract.UsageMetric;
import com.tingjian.contract.UsageReservationRequest;
import com.tingjian.contract.UsageReservationResponse;
import com.tingjian.contract.UsageSummaryResponse;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class UsageQuotaService {
    static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final Clock clock;
    private final Map<String, PlanTier> plans = new HashMap<>();
    private final Map<UserDayKey, Bucket> buckets = new HashMap<>();
    private final Map<String, Reservation> reservationsById = new HashMap<>();
    private final Map<IdempotencyKey, Reservation> reservationsByKey = new HashMap<>();

    public UsageQuotaService() {
        this(Clock.system(BUSINESS_ZONE));
    }

    UsageQuotaService(Clock clock) {
        this.clock = clock;
    }

    public synchronized UsageReservationResponse reserve(UsageReservationRequest request) {
        validate(request);
        IdempotencyKey key = new IdempotencyKey(request.userId().strip(), request.idempotencyKey().strip());
        Reservation existing = reservationsByKey.get(key);
        if (existing != null) {
            if (existing.metric != request.metric() || existing.amount != request.amount()) {
                throw new UsageException("IDEMPOTENCY_CONFLICT", "同一幂等键不能用于不同的用量请求");
            }
            return response(existing);
        }

        LocalDate day = LocalDate.now(clock);
        String userId = request.userId().strip();
        PlanTier plan = plans.getOrDefault(userId, PlanTier.FREE);
        Bucket bucket = buckets.computeIfAbsent(new UserDayKey(userId, day), ignored -> new Bucket());
        long remaining = remaining(bucket, plan, request.metric());
        UsageReservationResponse.ReservationStatus status = request.amount() <= remaining
                ? UsageReservationResponse.ReservationStatus.RESERVED
                : UsageReservationResponse.ReservationStatus.REJECTED;
        Reservation reservation = new Reservation(
                UUID.randomUUID().toString(), userId, day, request.metric(), request.amount(), status);
        if (status == UsageReservationResponse.ReservationStatus.RESERVED) {
            bucket.addReserved(request.metric(), request.amount());
        }
        reservationsById.put(reservation.id, reservation);
        reservationsByKey.put(key, reservation);
        return response(reservation);
    }

    public synchronized UsageReservationResponse commit(String reservationId) {
        Reservation reservation = requiredReservation(reservationId);
        if (reservation.status == UsageReservationResponse.ReservationStatus.COMMITTED) {
            return response(reservation);
        }
        if (reservation.status != UsageReservationResponse.ReservationStatus.RESERVED) {
            throw new UsageException("INVALID_RESERVATION_STATE", "只有已预占的用量可以确认");
        }
        Bucket bucket = requiredBucket(reservation);
        bucket.addReserved(reservation.metric, -reservation.amount);
        bucket.addUsed(reservation.metric, reservation.amount);
        reservation.status = UsageReservationResponse.ReservationStatus.COMMITTED;
        return response(reservation);
    }

    public synchronized UsageReservationResponse release(String reservationId) {
        Reservation reservation = requiredReservation(reservationId);
        if (reservation.status == UsageReservationResponse.ReservationStatus.RELEASED) {
            return response(reservation);
        }
        if (reservation.status != UsageReservationResponse.ReservationStatus.RESERVED) {
            throw new UsageException("INVALID_RESERVATION_STATE", "只有已预占的用量可以释放");
        }
        requiredBucket(reservation).addReserved(reservation.metric, -reservation.amount);
        reservation.status = UsageReservationResponse.ReservationStatus.RELEASED;
        return response(reservation);
    }

    public synchronized UsageSummaryResponse summary(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new UsageException("INVALID_USER", "用户 ID 不能为空");
        }
        String normalizedUserId = userId.strip();
        LocalDate day = LocalDate.now(clock);
        PlanTier plan = plans.getOrDefault(normalizedUserId, PlanTier.FREE);
        Bucket bucket = buckets.computeIfAbsent(new UserDayKey(normalizedUserId, day), ignored -> new Bucket());
        EnumMap<UsageMetric, UsageSummaryResponse.MetricUsage> metrics = new EnumMap<>(UsageMetric.class);
        for (UsageMetric metric : UsageMetric.values()) {
            long limit = UsagePolicy.limit(plan, metric);
            long used = bucket.used(metric);
            long reserved = bucket.reserved(metric);
            metrics.put(metric, new UsageSummaryResponse.MetricUsage(
                    used, reserved, limit, Math.max(0, limit - used - reserved)));
        }
        return new UsageSummaryResponse(normalizedUserId, plan, day.plusDays(1), Map.copyOf(metrics));
    }

    public synchronized UsageSummaryResponse setPlan(String userId, PlanTier plan) {
        if (userId == null || userId.isBlank() || plan == null) {
            throw new UsageException("INVALID_PLAN", "用户 ID 和套餐不能为空");
        }
        plans.put(userId.strip(), plan);
        return summary(userId);
    }

    private void validate(UsageReservationRequest request) {
        if (request == null || request.userId() == null || request.userId().isBlank()) {
            throw new UsageException("INVALID_USER", "用户 ID 不能为空");
        }
        if (request.metric() == null || request.amount() <= 0) {
            throw new UsageException("INVALID_AMOUNT", "用量类型不能为空且数量必须大于 0");
        }
        if (request.idempotencyKey() == null || request.idempotencyKey().isBlank()
                || request.idempotencyKey().length() > 128) {
            throw new UsageException("INVALID_IDEMPOTENCY_KEY", "幂等键不能为空且长度不能超过 128");
        }
    }

    private Reservation requiredReservation(String reservationId) {
        Reservation reservation = reservationsById.get(reservationId);
        if (reservation == null) {
            throw new UsageException("RESERVATION_NOT_FOUND", "用量预占记录不存在");
        }
        return reservation;
    }

    private Bucket requiredBucket(Reservation reservation) {
        Bucket bucket = buckets.get(new UserDayKey(reservation.userId, reservation.day));
        if (bucket == null) {
            throw new UsageException("RESERVATION_EXPIRED", "用量预占记录已过期");
        }
        return bucket;
    }

    private UsageReservationResponse response(Reservation reservation) {
        PlanTier plan = plans.getOrDefault(reservation.userId, PlanTier.FREE);
        Bucket bucket = buckets.getOrDefault(new UserDayKey(reservation.userId, reservation.day), new Bucket());
        return new UsageReservationResponse(
                reservation.id,
                reservation.status,
                reservation.metric,
                reservation.amount,
                remaining(bucket, plan, reservation.metric),
                reservation.day.plusDays(1));
    }

    private long remaining(Bucket bucket, PlanTier plan, UsageMetric metric) {
        return Math.max(0, UsagePolicy.limit(plan, metric) - bucket.used(metric) - bucket.reserved(metric));
    }

    private record UserDayKey(String userId, LocalDate day) {
    }

    private record IdempotencyKey(String userId, String key) {
    }

    private static final class Bucket {
        private final EnumMap<UsageMetric, Long> used = new EnumMap<>(UsageMetric.class);
        private final EnumMap<UsageMetric, Long> reserved = new EnumMap<>(UsageMetric.class);

        long used(UsageMetric metric) {
            return used.getOrDefault(metric, 0L);
        }

        long reserved(UsageMetric metric) {
            return reserved.getOrDefault(metric, 0L);
        }

        void addUsed(UsageMetric metric, long amount) {
            used.put(metric, used(metric) + amount);
        }

        void addReserved(UsageMetric metric, long amount) {
            reserved.put(metric, reserved(metric) + amount);
        }
    }

    private static final class Reservation {
        private final String id;
        private final String userId;
        private final LocalDate day;
        private final UsageMetric metric;
        private final long amount;
        private UsageReservationResponse.ReservationStatus status;

        private Reservation(String id, String userId, LocalDate day, UsageMetric metric, long amount,
                            UsageReservationResponse.ReservationStatus status) {
            this.id = id;
            this.userId = userId;
            this.day = day;
            this.metric = metric;
            this.amount = amount;
            this.status = status;
        }
    }
}
