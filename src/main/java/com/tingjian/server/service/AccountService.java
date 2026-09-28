package com.tingjian.server.service;

import com.tingjian.server.common.BusinessException;
import com.tingjian.server.common.ErrorCode;
import com.tingjian.server.dao.AuthSessionDao;
import com.tingjian.server.dao.PrivacyDao;
import com.tingjian.server.dao.UserDao;
import com.tingjian.server.entity.UserEntity;
import com.tingjian.server.util.PasswordHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public void delete(String userId, String password) {
        UserEntity user = userDao.findById(userId)
                .filter(candidate -> "ACTIVE".equals(candidate.status()))
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_TOKEN));
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
}
