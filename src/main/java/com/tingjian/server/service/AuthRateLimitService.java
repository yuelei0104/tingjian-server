package com.tingjian.server.service;

import com.tingjian.server.common.BusinessException;
import com.tingjian.server.common.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;

@Service
public class AuthRateLimitService {
    private final StringRedisTemplate redis;
    private final int maxLoginFailures;
    private final int maxIpLoginFailures;
    private final int maxCodeRequestsPerIp;
    private final Duration loginWindow;
    private final Duration codeCooldown;

    public AuthRateLimitService(
            StringRedisTemplate redis,
            @Value("${tingjian.auth.login-max-failures:5}") int maxLoginFailures,
            @Value("${tingjian.auth.login-max-ip-failures:25}") int maxIpLoginFailures,
            @Value("${tingjian.auth.code-max-requests-per-ip:30}") int maxCodeRequestsPerIp,
            @Value("${tingjian.auth.login-window-minutes:15}") long loginWindowMinutes,
            @Value("${tingjian.auth.code-cooldown-seconds:60}") long codeCooldownSeconds) {
        this.redis = redis;
        this.maxLoginFailures = maxLoginFailures;
        this.maxIpLoginFailures = maxIpLoginFailures;
        this.maxCodeRequestsPerIp = maxCodeRequestsPerIp;
        this.loginWindow = Duration.ofMinutes(loginWindowMinutes);
        this.codeCooldown = Duration.ofSeconds(codeCooldownSeconds);
    }

    public void checkLogin(String email, String clientIp) {
        if (count(loginEmailKey(email)) >= maxLoginFailures
                || count(loginIpKey(clientIp)) >= maxIpLoginFailures) {
            throw new BusinessException(ErrorCode.AUTH_TOO_MANY_ATTEMPTS);
        }
    }

    public void recordLoginFailure(String email, String clientIp) {
        for (String key : new String[]{loginEmailKey(email), loginIpKey(clientIp)}) {
            Long value = redis.opsForValue().increment(key);
            if (value != null && value == 1L) {
                redis.expire(key, loginWindow);
            }
        }
    }

    public void recordLoginSuccess(String email) {
        redis.delete(loginEmailKey(email));
    }

    public void checkAndRecordCodeRequest(String destination, String clientIp) {
        String destinationKey = "auth:code:cooldown:target:" + normalize(destination);
        String ipKey = "auth:code:daily:ip:" + safe(clientIp);
        Boolean destinationAccepted = redis.opsForValue()
                .setIfAbsent(destinationKey, "1", codeCooldown);
        Long ipCount = redis.opsForValue().increment(ipKey);
        if (ipCount != null && ipCount == 1L) {
            redis.expire(ipKey, Duration.ofDays(1));
        }
        if (!Boolean.TRUE.equals(destinationAccepted)
                || ipCount == null || ipCount > maxCodeRequestsPerIp) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_TOO_FREQUENT);
        }
    }

    private long count(String key) {
        String value = redis.opsForValue().get(key);
        return value == null ? 0 : Long.parseLong(value);
    }

    private String loginEmailKey(String email) {
        return "auth:login:fail:email:" + normalize(email);
    }

    private String loginIpKey(String clientIp) {
        return "auth:login:fail:ip:" + safe(clientIp);
    }

    private static String normalize(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value.strip();
    }
}
