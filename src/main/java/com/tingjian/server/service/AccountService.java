package com.tingjian.server.service;

import com.tingjian.server.common.BusinessException;
import com.tingjian.server.common.ErrorCode;
import com.tingjian.server.dao.AuthSessionDao;
import com.tingjian.server.dao.AuthSessionMetadataDao;
import com.tingjian.server.dao.PrivacyDao;
import com.tingjian.server.dao.UserDao;
import com.tingjian.server.dao.UserPhoneDao;
import com.tingjian.server.dto.PhoneBindingResponse;
import com.tingjian.server.dto.AuthUserResponse;
import com.tingjian.server.dto.AccountSessionResponse;
import com.tingjian.server.entity.UserEntity;
import com.tingjian.server.util.PasswordHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class AccountService {
    private final UserDao userDao;
    private final AuthSessionDao authSessionDao;
    private final PrivacyDao privacyDao;
    private final UserPhoneDao userPhoneDao;
    private final AuthSessionMetadataDao sessionMetadataDao;
    private final VerificationCodeService verificationCodeService;

    public AccountService(
            UserDao userDao,
            AuthSessionDao authSessionDao,
            PrivacyDao privacyDao,
            UserPhoneDao userPhoneDao,
            AuthSessionMetadataDao sessionMetadataDao,
            VerificationCodeService verificationCodeService) {
        this.userDao = userDao;
        this.authSessionDao = authSessionDao;
        this.privacyDao = privacyDao;
        this.userPhoneDao = userPhoneDao;
        this.sessionMetadataDao = sessionMetadataDao;
        this.verificationCodeService = verificationCodeService;
    }

    public AuthUserResponse profile(String userId) {
        return toResponse(requireActiveUser(userId));
    }

    public List<AccountSessionResponse> sessions(String userId, String currentSessionId) {
        requireActiveUser(userId);
        return authSessionDao.listActiveByUserId(userId, now()).stream()
                .map(session -> {
                    var metadata = sessionMetadataDao.findBySessionId(session.id()).orElse(null);
                    return new AccountSessionResponse(
                            session.id(), session.id().equals(currentSessionId),
                            metadata == null ? "未知设备" : metadata.deviceName(),
                            metadata == null ? "未知系统" : metadata.platform(),
                            metadata == null ? "" : metadata.appVersion(),
                            metadata == null ? "" : metadata.ipAddress(),
                            session.createdAt(), session.updatedAt(), session.refreshExpiresAt());
                })
                .toList();
    }

    public void revokeSession(String userId, String sessionId) {
        requireActiveUser(userId);
        authSessionDao.revokeByIdAndUserId(sessionId, userId, now());
    }

    public void revokeOtherSessions(String userId, String currentSessionId) {
        requireActiveUser(userId);
        authSessionDao.revokeOtherSessions(userId, currentSessionId, now());
    }

    public PhoneBindingResponse phone(String userId) {
        requireActiveUser(userId);
        return userPhoneDao.findByUserId(userId)
                .map(phone -> new PhoneBindingResponse(
                        true, maskPhone(phone.phone()), phone.verifiedAt()))
                .orElseGet(() -> new PhoneBindingResponse(false, null, null));
    }

    public com.tingjian.server.dto.VerificationChallengeResponse requestPhoneCode(
            String userId, String phone, String clientIp) {
        requireActiveUser(userId);
        String normalized = phone.strip().replace(" ", "");
        userPhoneDao.findUserIdByPhone(normalized)
                .filter(ownerId -> !ownerId.equals(userId))
                .ifPresent(ownerId -> {
                    throw new BusinessException(ErrorCode.PHONE_ALREADY_BOUND);
                });
        return verificationCodeService.issueSms(
                normalized, VerificationCodeService.BIND_PHONE, clientIp, true);
    }

    @Transactional
    public PhoneBindingResponse bindPhone(
            String userId, String phone, String verificationId, String code) {
        requireActiveUser(userId);
        verificationCodeService.verifySmsAndConsume(
                verificationId, code, phone, VerificationCodeService.BIND_PHONE);
        LocalDateTime now = now();
        try {
            userPhoneDao.upsert(userId, phone.strip().replace(" ", ""), now);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_BOUND);
        }
        return phone(userId);
    }

    @Transactional
    public void unbindPhone(String userId, String password) {
        UserEntity user = requireActiveUser(userId);
        if (!PasswordHasher.matches(password, user.passwordHash())) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
        userPhoneDao.deleteByUserId(userId);
    }

    @Transactional
    public AuthUserResponse updateProfile(String userId, String displayName) {
        requireActiveUser(userId);
        String normalizedName = displayName.strip();
        if (userDao.updateDisplayName(userId, normalizedName, now()) == 0) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }
        return profile(userId);
    }

    @Transactional
    public void changePassword(String userId, String currentPassword, String newPassword) {
        UserEntity user = requireActiveUser(userId);
        if (!PasswordHasher.matches(currentPassword, user.passwordHash())) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
        if (PasswordHasher.matches(newPassword, user.passwordHash())) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR, "新密码不能与当前密码相同");
        }
        if (userDao.updatePassword(userId, PasswordHasher.hash(newPassword), now()) == 0) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }
        authSessionDao.deleteByUserId(userId);
    }

    @Transactional
    public void delete(String userId, String password) {
        UserEntity user = requireActiveUser(userId);
        if (!PasswordHasher.matches(password, user.passwordHash())) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        // These tables predate account foreign keys, so remove owned data explicitly.
        privacyDao.deleteConversationMessages(userId);
        privacyDao.deleteConversations(userId);
        privacyDao.deleteKeywords(userId);
        privacyDao.deleteGlossaryTerms(userId);
        privacyDao.deleteQuickPhrases(userId);
        privacyDao.deleteAiSuggestionRequests(userId);
        privacyDao.deleteUserPreference(userId);
        privacyDao.deleteAccessibilityPreference(userId);
        authSessionDao.deleteByUserId(userId);
        if (userDao.delete(userId) == 0) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }
    }

    private UserEntity requireActiveUser(String userId) {
        return userDao.findById(userId)
                .filter(candidate -> "ACTIVE".equals(candidate.status()))
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_TOKEN));
    }

    private static AuthUserResponse toResponse(UserEntity user) {
        return new AuthUserResponse(
                user.id(), user.email(), user.displayName(), user.createdAt());
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private static String maskPhone(String phone) {
        if (phone.length() <= 7) {
            return "****";
        }
        return phone.substring(0, Math.min(3, phone.length() - 4))
                + "****" + phone.substring(phone.length() - 4);
    }
}
