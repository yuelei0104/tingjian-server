package com.tingjian.server.service;

import com.tingjian.server.common.BusinessException;
import com.tingjian.server.common.ErrorCode;
import com.tingjian.server.dao.AuthSessionDao;
import com.tingjian.server.dao.UserDao;
import com.tingjian.server.dto.AuthTokenResponse;
import com.tingjian.server.dto.AuthUserResponse;
import com.tingjian.server.dto.LoginRequest;
import com.tingjian.server.dto.RegisterRequest;
import com.tingjian.server.entity.AuthSessionEntity;
import com.tingjian.server.entity.UserEntity;
import com.tingjian.server.util.IdGenerator;
import com.tingjian.server.util.PasswordHasher;
import com.tingjian.server.util.TokenGenerator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

@Service
public class AuthService {
    private static final int ACCESS_TOKEN_MINUTES = 30;
    private static final int REFRESH_TOKEN_DAYS = 30;

    private final UserDao userDao;
    private final AuthSessionDao authSessionDao;

    public AuthService(UserDao userDao, AuthSessionDao authSessionDao) {
        this.userDao = userDao;
        this.authSessionDao = authSessionDao;
    }

    @Transactional
    public AuthTokenResponse register(RegisterRequest request) {
        LocalDateTime now = now();
        UserEntity user = new UserEntity(
                IdGenerator.uuid(), normalizeEmail(request.email()), PasswordHasher.hash(request.password()),
                request.displayName().strip(), "ACTIVE", now, now);
        try {
            userDao.create(user);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        return createSession(user, now);
    }

    @Transactional
    public AuthTokenResponse login(LoginRequest request) {
        UserEntity user = userDao.findByEmail(normalizeEmail(request.email()))
                .filter(candidate -> "ACTIVE".equals(candidate.status()))
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));
        if (!PasswordHasher.matches(request.password(), user.passwordHash())) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
        return createSession(user, now());
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

    private AuthTokenResponse createSession(UserEntity user, LocalDateTime now) {
        TokenPair pair = newTokenPair(now);
        AuthSessionEntity session = new AuthSessionEntity(
                IdGenerator.uuid(), user.id(), TokenGenerator.hash(pair.accessToken()),
                TokenGenerator.hash(pair.refreshToken()), pair.accessExpiresAt(), pair.refreshExpiresAt(),
                null, now, now);
        authSessionDao.create(session);
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
