package com.tingjian.server.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AccessTokenInterceptor implements HandlerInterceptor {
    public static final String USER_ID_ATTRIBUTE = AccessTokenInterceptor.class.getName() + ".userId";
    public static final String SESSION_ID_ATTRIBUTE =
            AccessTokenInterceptor.class.getName() + ".sessionId";
    private final AccessTokenService accessTokenService;

    public AccessTokenInterceptor(AccessTokenService accessTokenService) {
        this.accessTokenService = accessTokenService;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        AuthPrincipal principal = accessTokenService.resolvePrincipal(
                request.getHeader("Authorization"));
        request.setAttribute(USER_ID_ATTRIBUTE, principal.userId());
        request.setAttribute(SESSION_ID_ATTRIBUTE, principal.sessionId());
        return true;
    }
}
