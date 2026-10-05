package com.tingjian.server.service;

import com.tingjian.server.common.BusinessException;
import com.tingjian.server.common.ErrorCode;
import com.tingjian.server.dao.VerificationCodeDao;
import com.tingjian.server.dto.VerificationChallengeResponse;
import com.tingjian.server.entity.VerificationCodeEntity;
import com.tingjian.server.util.IdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class VerificationCodeService {
    public static final String REGISTER = "REGISTER";
    public static final String RESET_PASSWORD = "RESET_PASSWORD";
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final VerificationCodeDao dao;
    private final VerificationDeliveryGateway deliveryGateway;
    private final AuthRateLimitService rateLimitService;
    private final byte[] pepper;
    private final int validMinutes;

    public VerificationCodeService(
            VerificationCodeDao dao,
            VerificationDeliveryGateway deliveryGateway,
            AuthRateLimitService rateLimitService,
            @Value("${tingjian.auth.verification-pepper:change-this-development-pepper}") String pepper,
            @Value("${tingjian.auth.verification-valid-minutes:10}") int validMinutes) {
        this.dao = dao;
        this.deliveryGateway = deliveryGateway;
        this.rateLimitService = rateLimitService;
        this.pepper = pepper.getBytes(StandardCharsets.UTF_8);
        this.validMinutes = validMinutes;
    }

    @Transactional
    public VerificationChallengeResponse issueEmail(
            String email, String purpose, String clientIp, boolean deliver) {
        String destination = normalizeEmail(email);
        rateLimitService.checkAndRecordCodeRequest(destination, clientIp);
        String id = IdGenerator.uuid();
        String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
        LocalDateTime now = now();
        LocalDateTime expiresAt = now.plusMinutes(validMinutes);
        dao.create(new VerificationCodeEntity(
                id, "EMAIL", destination, purpose, hash(id, code), 0,
                expiresAt, null, now));
        if (deliver) {
            deliveryGateway.send("EMAIL", destination, purpose, code, validMinutes);
        }
        return new VerificationChallengeResponse(id, expiresAt);
    }

    @Transactional
    public void verifyAndConsume(
            String verificationId, String code, String destination, String purpose) {
        LocalDateTime now = now();
        VerificationCodeEntity challenge = dao.findForUpdate(verificationId)
                .orElseThrow(this::invalidCode);
        boolean metadataMatches = challenge.consumedAt() == null
                && challenge.expiresAt().isAfter(now)
                && challenge.failedAttempts() < MAX_FAILED_ATTEMPTS
                && challenge.destination().equals(normalizeEmail(destination))
                && challenge.purpose().equals(purpose)
                && challenge.channel().equals("EMAIL");
        if (!metadataMatches || !constantTimeEquals(challenge.codeHash(), hash(verificationId, code))) {
            if (challenge.consumedAt() == null) {
                dao.incrementFailedAttempts(verificationId);
            }
            throw invalidCode();
        }
        if (dao.consume(verificationId, now) == 0) {
            throw invalidCode();
        }
    }

    private String hash(String id, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pepper, "HmacSHA256"));
            return HexFormat.of().formatHex(
                    mac.doFinal((id + ':' + code).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Verification code hashing is unavailable", exception);
        }
    }

    private static boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.US_ASCII), right.getBytes(StandardCharsets.US_ASCII));
    }

    private BusinessException invalidCode() {
        return new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID);
    }

    private static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
