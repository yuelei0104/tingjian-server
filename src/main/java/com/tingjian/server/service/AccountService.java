package com.tingjian.server.service;

import com.tingjian.server.common.BusinessException;
import com.tingjian.server.common.ErrorCode;
import com.tingjian.server.dao.AuthSessionDao;
import com.tingjian.server.dao.PrivacyDao;
import com.tingjian.server.dao.UserDao;
import com.tingjian.server.dto.AuthUserResponse;
import com.tingjian.server.dto.AccountSessionResponse;
import com.tingjian.server.entity.UserEntity;
import com.tingjian.server.util.PasswordHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class AccountService {
    private final UserDao userDao;
    private final AuthSessionDao authSessionDao;
    private final PrivacyDao privacyDao;

    public AccountService(
            UserDao userDao,
            AuthSessionDao authSessionDao,
            PrivacyDao privacyDao) {
        this.userDao = userDao;
        this.authSessionDao = authSessionDao;
        this.privacyDao = privacyDao;
    }

    public AuthUserResponse profile(String userId) {
        return toResponse(requireActiveUser(userId));
    }

    public List<AccountSessionResponse> sessions(String userId) {
        requireActiveUser(userId);
        return authSessionDao.listActiveByUserId(userId, now()).stream()
                .map(session -> new AccountSessionResponse(
                        session.id(), session.createdAt(), session.updatedAt(),
                        session.refreshExpiresAt()))
                .toList();
    }

    public void revokeSession(String userId, String sessionId) {
        requireActiveUser(userId);
        authSessionDao.revokeByIdAndUserId(sessionId, userId, now());
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
        privacyDao.deleteUserPreference(userId);
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
}
