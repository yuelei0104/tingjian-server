package com.tingjian.server.config;

import com.tingjian.server.common.BusinessException;
import com.tingjian.server.common.ErrorCode;
import com.tingjian.server.dao.AuthSessionDao;
import com.tingjian.server.util.TokenGenerator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class AccessTokenService {
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthSessionDao authSessionDao;

    public AccessTokenService(AuthSessionDao authSessionDao) {
        this.authSessionDao = authSessionDao;
    }

    public String resolveUserId(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }
        String accessToken = authorization.substring(BEARER_PREFIX.length()).strip();
        if (accessToken.isEmpty()) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }
        return authSessionDao.findActiveUserIdByAccessToken(
                        TokenGenerator.hash(accessToken), LocalDateTime.now(ZoneOffset.UTC))
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_TOKEN));
    }
}
