package com.tingjian.usage;

import com.tingjian.contract.PlanTier;
import com.tingjian.contract.UsageMetric;
import com.tingjian.contract.UsageReservationRequest;
import com.tingjian.contract.UsageReservationResponse;
import com.tingjian.contract.UsageReservationResponse.ReservationStatus;
import com.tingjian.contract.UsageSummaryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class UsageQuotaService {
    static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final UsageQuotaDao dao;
    private final UsageIdempotencyIndex idempotencyIndex;
    private final Clock clock;
    private final Duration reservationTtl;

    public UsageQuotaService(
            UsageQuotaDao dao,
            UsageIdempotencyIndex idempotencyIndex,
            Clock clock,
            @Value("${tingjian.usage.reservation-ttl:5m}") Duration reservationTtl) {
        this.dao = dao;
        this.idempotencyIndex = idempotencyIndex;
        this.clock = clock;
        this.reservationTtl = reservationTtl;
    }

    @Transactional
    public UsageReservationResponse reserve(UsageReservationRequest request) {
        validate(request);
        String userId = request.userId().strip();
        String idempotencyKey = request.idempotencyKey().strip();
        LocalDate day = LocalDate.now(clock);
        Instant now = clock.instant();
        PlanTier plan = dao.lockPlan(userId);
        dao.releaseExpired(userId, day, now);

        Optional<UsageQuotaDao.ReservationRecord> existing = idempotencyIndex
                .findReservationId(userId, idempotencyKey)
                .flatMap(dao::findById)
                .filter(reservation -> reservation.userId().equals(userId))
                .or(() -> dao.findByIdempotency(userId, idempotencyKey));
        if (existing.isPresent()) {
            UsageQuotaDao.ReservationRecord reservation = existing.get();
            if (reservation.status() == ReservationStatus.RESERVED
                    && !reservation.expiresAt().isAfter(now)) {
                dao.releaseExpired(userId, reservation.day(), now);
                reservation = dao.findById(reservation.id()).orElseThrow();
            }
            ensureSameRequest(reservation, request);
            idempotencyIndex.remember(userId, idempotencyKey, reservation.id());
            return response(reservation, plan);
        }

        UsageQuotaDao.Bucket bucket = dao.lockBucket(userId, day, request.metric());
        long remaining = remaining(bucket, plan, request.metric());
        ReservationStatus status = request.amount() <= remaining
                ? ReservationStatus.RESERVED
                : ReservationStatus.REJECTED;
        UsageQuotaDao.ReservationRecord reservation = new UsageQuotaDao.ReservationRecord(
                UUID.randomUUID().toString(), userId, day, request.metric(), request.amount(),
                idempotencyKey, status, now.plus(reservationTtl));
        dao.insertReservation(reservation);
        if (status == ReservationStatus.RESERVED) {
            dao.updateBucket(userId, day, request.metric(),
                    new UsageQuotaDao.Bucket(
                            bucket.used(), bucket.reserved() + request.amount()));
        }
        idempotencyIndex.remember(userId, idempotencyKey, reservation.id());
        return response(reservation, plan);
    }

    @Transactional
    public UsageReservationResponse commit(String reservationId) {
        UsageQuotaDao.ReservationRecord preview = requiredReservation(reservationId);
        PlanTier plan = dao.lockPlan(preview.userId());
        UsageQuotaDao.ReservationRecord reservation = dao.lockReservation(reservationId);
        if (reservation.status() == ReservationStatus.COMMITTED) {
            return response(reservation, plan);
        }
        if (reservation.status() != ReservationStatus.RESERVED) {
            throw invalidState();
        }
        if (!reservation.expiresAt().isAfter(clock.instant())) {
            UsageQuotaDao.Bucket expiredBucket = dao.lockBucket(
                    reservation.userId(), reservation.day(), reservation.metric());
            dao.updateBucket(reservation.userId(), reservation.day(), reservation.metric(),
                    new UsageQuotaDao.Bucket(
                            expiredBucket.used(),
                            Math.max(0, expiredBucket.reserved() - reservation.amount())));
            dao.updateReservationStatus(reservation.id(), ReservationStatus.RELEASED);
            return response(withStatus(reservation, ReservationStatus.RELEASED), plan);
        }

        UsageQuotaDao.Bucket bucket = dao.lockBucket(
                reservation.userId(), reservation.day(), reservation.metric());
        dao.updateBucket(reservation.userId(), reservation.day(), reservation.metric(),
                new UsageQuotaDao.Bucket(
                        bucket.used() + reservation.amount(),
                        Math.max(0, bucket.reserved() - reservation.amount())));
        dao.updateReservationStatus(reservation.id(), ReservationStatus.COMMITTED);
        return response(withStatus(reservation, ReservationStatus.COMMITTED), plan);
    }

    @Transactional
    public UsageReservationResponse release(String reservationId) {
        UsageQuotaDao.ReservationRecord preview = requiredReservation(reservationId);
        PlanTier plan = dao.lockPlan(preview.userId());
        UsageQuotaDao.ReservationRecord reservation = dao.lockReservation(reservationId);
        if (reservation.status() == ReservationStatus.RELEASED) {
            return response(reservation, plan);
        }
        if (reservation.status() != ReservationStatus.RESERVED) {
            throw invalidState();
        }
        UsageQuotaDao.Bucket bucket = dao.lockBucket(
                reservation.userId(), reservation.day(), reservation.metric());
        dao.updateBucket(reservation.userId(), reservation.day(), reservation.metric(),
                new UsageQuotaDao.Bucket(
                        bucket.used(), Math.max(0, bucket.reserved() - reservation.amount())));
        dao.updateReservationStatus(reservation.id(), ReservationStatus.RELEASED);
        return response(withStatus(reservation, ReservationStatus.RELEASED), plan);
    }

    @Transactional
    public UsageSummaryResponse summary(String userId) {
        String normalizedUserId = requireUser(userId);
        LocalDate day = LocalDate.now(clock);
        PlanTier plan = dao.lockPlan(normalizedUserId);
        dao.releaseExpired(normalizedUserId, day, clock.instant());
        Map<UsageMetric, UsageQuotaDao.Bucket> buckets = dao.buckets(normalizedUserId, day);
        EnumMap<UsageMetric, UsageSummaryResponse.MetricUsage> metrics =
                new EnumMap<>(UsageMetric.class);
        for (UsageMetric metric : UsageMetric.values()) {
            UsageQuotaDao.Bucket bucket = buckets.getOrDefault(
                    metric, new UsageQuotaDao.Bucket(0, 0));
            long limit = UsagePolicy.limit(plan, metric);
            metrics.put(metric, new UsageSummaryResponse.MetricUsage(
                    bucket.used(), bucket.reserved(), limit,
                    Math.max(0, limit - bucket.used() - bucket.reserved())));
        }
        return new UsageSummaryResponse(
                normalizedUserId, plan, day.plusDays(1), Map.copyOf(metrics));
    }

    @Transactional
    public UsageSummaryResponse setPlan(String userId, PlanTier plan) {
        String normalizedUserId = requireUser(userId);
        if (plan == null) throw new UsageException("INVALID_PLAN", "套餐不能为空");
        dao.lockPlan(normalizedUserId);
        dao.updatePlan(normalizedUserId, plan);
        return summary(normalizedUserId);
    }

    @Transactional
    public int releaseExpiredReservations() {
        Instant now = clock.instant();
        int released = 0;
        List<UsageQuotaDao.UserDay> userDays = dao.expiredUserDays(now, 100);
        for (UsageQuotaDao.UserDay userDay : userDays) {
            dao.lockPlan(userDay.userId());
            released += dao.releaseExpired(userDay.userId(), userDay.day(), now);
        }
        return released;
    }

    private UsageReservationResponse response(
            UsageQuotaDao.ReservationRecord reservation, PlanTier plan) {
        UsageQuotaDao.Bucket bucket = dao.lockBucket(
                reservation.userId(), reservation.day(), reservation.metric());
        return new UsageReservationResponse(
                reservation.id(), reservation.status(), reservation.metric(), reservation.amount(),
                remaining(bucket, plan, reservation.metric()), reservation.day().plusDays(1));
    }

    private static UsageQuotaDao.ReservationRecord withStatus(
            UsageQuotaDao.ReservationRecord reservation, ReservationStatus status) {
        return new UsageQuotaDao.ReservationRecord(
                reservation.id(), reservation.userId(), reservation.day(), reservation.metric(),
                reservation.amount(), reservation.idempotencyKey(), status, reservation.expiresAt());
    }

    private static void ensureSameRequest(
            UsageQuotaDao.ReservationRecord existing, UsageReservationRequest request) {
        if (existing.metric() != request.metric() || existing.amount() != request.amount()) {
            throw new UsageException(
                    "IDEMPOTENCY_CONFLICT", "同一幂等键不能用于不同的用量请求");
        }
    }

    private static long remaining(
            UsageQuotaDao.Bucket bucket, PlanTier plan, UsageMetric metric) {
        return Math.max(0,
                UsagePolicy.limit(plan, metric) - bucket.used() - bucket.reserved());
    }

    private static String requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new UsageException("INVALID_USER", "用户 ID 不能为空");
        }
        return userId.strip();
    }

    private UsageQuotaDao.ReservationRecord requiredReservation(String reservationId) {
        return dao.findById(reservationId).orElseThrow(() -> new UsageException(
                "RESERVATION_NOT_FOUND", "用量预占记录不存在"));
    }

    private static void validate(UsageReservationRequest request) {
        if (request == null) throw new UsageException("INVALID_USER", "用户 ID 不能为空");
        requireUser(request.userId());
        if (request.metric() == null || request.amount() <= 0) {
            throw new UsageException(
                    "INVALID_AMOUNT", "用量类型不能为空且数量必须大于 0");
        }
        if (request.idempotencyKey() == null || request.idempotencyKey().isBlank()
                || request.idempotencyKey().length() > 128) {
            throw new UsageException(
                    "INVALID_IDEMPOTENCY_KEY", "幂等键不能为空且长度不能超过 128");
        }
    }

    private static UsageException invalidState() {
        return new UsageException(
                "INVALID_RESERVATION_STATE", "只有已预占的用量可以确认或释放");
    }
}
