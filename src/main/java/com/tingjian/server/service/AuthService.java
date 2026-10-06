package com.tingjian.server.service;

import com.tingjian.server.common.BusinessException;
import com.tingjian.server.common.ErrorCode;
import com.tingjian.server.dao.AuthSessionDao;
import com.tingjian.server.dao.AuthSessionMetadataDao;
import com.tingjian.server.dao.UserDao;
import com.tingjian.server.dao.UserPhoneDao;
import com.tingjian.server.dto.AuthClientInfo;
import com.tingjian.server.dto.AuthTokenResponse;
import com.tingjian.server.dto.AuthUserResponse;
import com.tingjian.server.dto.LoginRequest;
import com.tingjian.server.dto.RegisterRequest;
import com.tingjian.server.dto.SmsPasswordResetRequest;
import com.tingjian.server.entity.AuthSessionMetadataEntity;
import com.tingjian.server.entity.AuthSessionEntity;
import com.tingjian.server.entity.UserEntity;
import com.tingjian.server.util.IdGenerator;
import com.tingjian.server.util.PasswordHasher;
import com.tingjian.server.util.TokenGenerator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int ACCESS_TOKEN_MINUTES = 30;
    private static final int REFRESH_TOKEN_DAYS = 30;

    private final UserDao userDao;
    private final AuthSessionDao authSessionDao;
    private final VerificationCodeService verificationCodeService;
    private final AuthRateLimitService rateLimitService;
    private final UserPhoneDao userPhoneDao;
    private final AuthSessionMetadataDao sessionMetadataDao;
    private final SecurityNotificationGateway notificationGateway;

    public AuthService(
            UserDao userDao,
            AuthSessionDao authSessionDao,
            VerificationCodeService verificationCodeService,
            AuthRateLimitService rateLimitService,
            UserPhoneDao userPhoneDao,
            AuthSessionMetadataDao sessionMetadataDao,
            SecurityNotificationGateway notificationGateway) {
        this.userDao = userDao;
        this.authSessionDao = authSessionDao;
        this.verificationCodeService = verificationCodeService;
        this.rateLimitService = rateLimitService;
        this.userPhoneDao = userPhoneDao;
        this.sessionMetadataDao = sessionMetadataDao;
        this.notificationGateway = notificationGateway;
    }

    @Transactional
    public AuthTokenResponse register(RegisterRequest request) {
        return register(request, unknownClient());
    }

    @Transactional
    public AuthTokenResponse register(RegisterRequest request, AuthClientInfo clientInfo) {
        String email = normalizeEmail(request.email());
        verificationCodeService.verifyAndConsume(
                request.verificationId(), request.verificationCode(), email,
                VerificationCodeService.REGISTER);
        LocalDateTime now = now();
        UserEntity user = new UserEntity(
                IdGenerator.uuid(), email, PasswordHasher.hash(request.password()),
                request.displayName().strip(), "ACTIVE", now, now);
        try {
            userDao.create(user);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        return createSession(user, now, clientInfo, false);
    }

    @Transactional
    public AuthTokenResponse login(LoginRequest request, String clientIp) {
        return login(request, new AuthClientInfo(
                "", "未知设备", "未知系统", "", clientIp));
    }

    @Transactional
    public AuthTokenResponse login(LoginRequest request, AuthClientInfo clientInfo) {
        String email = normalizeEmail(request.email());
        rateLimitService.checkLogin(email, clientInfo.ipAddress());
        UserEntity user = userDao.findByEmail(email)
                .filter(candidate -> "ACTIVE".equals(candidate.status()))
                .orElse(null);
        if (user == null || !PasswordHasher.matches(request.password(), user.passwordHash())) {
            rateLimitService.recordLoginFailure(email, clientInfo.ipAddress());
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
        rateLimitService.recordLoginSuccess(email);
        return createSession(user, now(), clientInfo, true);
    }

    public com.tingjian.server.dto.VerificationChallengeResponse requestRegistrationCode(
            String email, String clientIp) {
        String normalized = normalizeEmail(email);
        if (userDao.findByEmail(normalized).isPresent()) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        return verificationCodeService.issueEmail(
                normalized, VerificationCodeService.REGISTER, clientIp, true);
    }

    public com.tingjian.server.dto.VerificationChallengeResponse requestPasswordReset(
            String email, String clientIp) {
        String normalized = normalizeEmail(email);
        boolean userExists = userDao.findByEmail(normalized)
                .filter(user -> "ACTIVE".equals(user.status()))
                .isPresent();
        return verificationCodeService.issueEmail(
                normalized, VerificationCodeService.RESET_PASSWORD, clientIp, userExists);
    }

    public com.tingjian.server.dto.VerificationChallengeResponse requestSmsPasswordReset(
            String phone, String clientIp) {
        String normalized = normalizePhone(phone);
        boolean phoneExists = userPhoneDao.findUserIdByPhone(normalized).isPresent();
        return verificationCodeService.issueSms(
                normalized, VerificationCodeService.RESET_PASSWORD, clientIp, phoneExists);
    }

    @Transactional
    public void resetPassword(com.tingjian.server.dto.PasswordResetRequest request) {
        String email = normalizeEmail(request.email());
        verificationCodeService.verifyAndConsume(
                request.verificationId(), request.verificationCode(), email,
                VerificationCodeService.RESET_PASSWORD);
        UserEntity user = userDao.findByEmail(email)
                .filter(candidate -> "ACTIVE".equals(candidate.status()))
                .orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID));
        if (userDao.updatePassword(
                user.id(), PasswordHasher.hash(request.newPassword()), now()) == 0) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID);
        }
        authSessionDao.deleteByUserId(user.id());
    }

    @Transactional
    public void resetPasswordBySms(SmsPasswordResetRequest request) {
        String phone = normalizePhone(request.phone());
        verificationCodeService.verifySmsAndConsume(
                request.verificationId(), request.verificationCode(), phone,
                VerificationCodeService.RESET_PASSWORD);
        String userId = userPhoneDao.findUserIdByPhone(phone)
                .orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID));
        UserEntity user = userDao.findById(userId)
                .filter(candidate -> "ACTIVE".equals(candidate.status()))
                .orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID));
        if (userDao.updatePassword(
                user.id(), PasswordHasher.hash(request.newPassword()), now()) == 0) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID);
        }
        authSessionDao.deleteByUserId(user.id());
    }

    @Transactional
    public AuthTokenResponse refresh(String refreshToken) {
        LocalDateTime now = now();
        AuthSessionEntity current = authSessionDao.findActiveByRefreshTokenForUpdate(
                        TokenGenerator.hash(refreshToken), now)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_TOKEN));
        UserEntity user = userDao.findById(current.userId())
                .filter(candidate -> "ACTIVE".equals(candidate.status()))
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_TOKEN));

        TokenPair pair = newTokenPair(now);
        AuthSessionEntity rotated = new AuthSessionEntity(
                current.id(), current.userId(), TokenGenerator.hash(pair.accessToken()),
                TokenGenerator.hash(pair.refreshToken()), pair.accessExpiresAt(), pair.refreshExpiresAt(),
                null, current.createdAt(), now);
        if (authSessionDao.rotate(rotated) == 0) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }
        return toResponse(pair, user);
    }

    public void logout(String refreshToken) {
        authSessionDao.revokeByRefreshToken(TokenGenerator.hash(refreshToken), now());
    }

    private AuthTokenResponse createSession(
            UserEntity user, LocalDateTime now, AuthClientInfo clientInfo, boolean notifyNewDevice) {
        TokenPair pair = newTokenPair(now);
        AuthSessionEntity session = new AuthSessionEntity(
                IdGenerator.uuid(), user.id(), TokenGenerator.hash(pair.accessToken()),
                TokenGenerator.hash(pair.refreshToken()), pair.accessExpiresAt(), pair.refreshExpiresAt(),
                null, now, now);
        authSessionDao.create(session);
        String deviceHash = clientInfo.deviceId() == null || clientInfo.deviceId().isBlank()
                ? null : TokenGenerator.hash(clientInfo.deviceId());
        boolean knownDevice = sessionMetadataDao.isKnownDevice(user.id(), deviceHash);
        sessionMetadataDao.create(new AuthSessionMetadataEntity(
                session.id(), user.id(), deviceHash,
                limited(clientInfo.deviceName(), 80, "未知设备"),
                limited(clientInfo.platform(), 40, "未知系统"),
                limited(clientInfo.appVersion(), 24, ""),
                limited(clientInfo.ipAddress(), 64, "unknown"), now));
        if (notifyNewDevice && !knownDevice && deviceHash != null) {
            try {
                notificationGateway.newLogin(
                        user.email(), clientInfo.deviceName(), clientInfo.platform(),
                        clientInfo.ipAddress(), now);
            } catch (RuntimeException exception) {
                log.warn("Unable to deliver new-login notification for user {}", user.id(), exception);
            }
        }
        return toResponse(pair, user);
    }

    private static TokenPair newTokenPair(LocalDateTime now) {
        return new TokenPair(
                TokenGenerator.token(), TokenGenerator.token(),
                now.plusMinutes(ACCESS_TOKEN_MINUTES), now.plusDays(REFRESH_TOKEN_DAYS));
    }

    private static AuthTokenResponse toResponse(TokenPair pair, UserEntity user) {
        return new AuthTokenResponse(
                pair.accessToken(), pair.refreshToken(), pair.accessExpiresAt(), pair.refreshExpiresAt(),
                new AuthUserResponse(user.id(), user.email(), user.displayName(), user.createdAt()));
    }

    private static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private static String normalizePhone(String phone) {
        return phone.strip().replace(" ", "");
    }

    private static AuthClientInfo unknownClient() {
        return new AuthClientInfo("", "未知设备", "未知系统", "", "unknown");
    }

    private static String limited(String value, int max, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String stripped = value.strip();
        return stripped.length() <= max ? stripped : stripped.substring(0, max);
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private record TokenPair(
            String accessToken,
            String refreshToken,
            LocalDateTime accessExpiresAt,
            LocalDateTime refreshExpiresAt) {
    }
}
