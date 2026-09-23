package com.tingjian.server.config;

import com.tingjian.server.common.BusinessException;
import com.tingjian.server.common.ErrorCode;
import com.tingjian.server.dao.AuthSessionDao;
import com.tingjian.server.util.TokenGenerator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class AccessTokenInterceptor implements HandlerInterceptor {
    public static final String USER_ID_ATTRIBUTE = AccessTokenInterceptor.class.getName() + ".userId";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthSessionDao authSessionDao;

    public AccessTokenInterceptor(AuthSessionDao authSessionDao) {
        this.authSessionDao = authSessionDao;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        String accessToken = authorization.substring(BEARER_PREFIX.length()).strip();
        if (accessToken.isEmpty()) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        String userId = authSessionDao.findActiveUserIdByAccessToken(
                        TokenGenerator.hash(accessToken), LocalDateTime.now(ZoneOffset.UTC))
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_TOKEN));
        request.setAttribute(USER_ID_ATTRIBUTE, userId);
        return true;
    }
}
