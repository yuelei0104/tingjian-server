package com.tingjian.usage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

@Component
@ConditionalOnProperty(
        prefix = "tingjian.usage.redis-cache",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
class RedisUsageIdempotencyIndex implements UsageIdempotencyIndex {
    private static final Logger log = LoggerFactory.getLogger(RedisUsageIdempotencyIndex.class);
    private final StringRedisTemplate redis;
    private final Duration ttl;

    RedisUsageIdempotencyIndex(
            StringRedisTemplate redis,
            @Value("${tingjian.usage.redis-cache.ttl:2d}") Duration ttl) {
        this.redis = redis;
        this.ttl = ttl;
    }

    @Override
    public Optional<String> findReservationId(String userId, String idempotencyKey) {
        try {
            return Optional.ofNullable(redis.opsForValue().get(key(userId, idempotencyKey)));
        } catch (RuntimeException exception) {
            log.warn("Redis idempotency lookup unavailable; falling back to MySQL");
            return Optional.empty();
        }
    }

    @Override
    public void remember(String userId, String idempotencyKey, String reservationId) {
        try {
            redis.opsForValue().set(key(userId, idempotencyKey), reservationId, ttl);
        } catch (RuntimeException exception) {
            log.warn("Redis idempotency write unavailable; MySQL remains authoritative");
        }
    }

    private static String key(String userId, String idempotencyKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((userId + "\u0000" + idempotencyKey)
                    .getBytes(StandardCharsets.UTF_8));
            return "tingjian:usage:idempotency:" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
