package com.tingjian.server.config;

import com.tingjian.server.common.BusinessException;
import com.tingjian.server.dao.AuthSessionDao;
import com.tingjian.server.util.TokenGenerator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccessTokenInterceptorTests {
    private final AuthSessionDao authSessionDao = mock(AuthSessionDao.class);
    private final AccessTokenInterceptor interceptor = new AccessTokenInterceptor(authSessionDao);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);

    @Test
    void validBearerTokenAddsCurrentUserToRequest() {
        when(request.getHeader("Authorization")).thenReturn("Bearer access-token");
        when(authSessionDao.findActiveUserIdByAccessToken(any(), any(LocalDateTime.class)))
                .thenReturn(Optional.of("user-1"));

        assertTrue(interceptor.preHandle(request, response, new Object()));

        verify(authSessionDao).findActiveUserIdByAccessToken(
                org.mockito.ArgumentMatchers.eq(TokenGenerator.hash("access-token")),
                any(LocalDateTime.class));
        verify(request).setAttribute(AccessTokenInterceptor.USER_ID_ATTRIBUTE, "user-1");
    }

    @Test
    void missingBearerTokenIsRejected() {
        when(request.getHeader("Authorization")).thenReturn(null);

        var exception = assertThrows(BusinessException.class,
                () -> interceptor.preHandle(request, response, new Object()));

        assertEquals("AUTH_INVALID_TOKEN", exception.errorCode().name());
    }

    @Test
    void expiredOrRevokedTokenIsRejected() {
        when(request.getHeader("Authorization")).thenReturn("Bearer expired-token");
        when(authSessionDao.findActiveUserIdByAccessToken(any(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        var exception = assertThrows(BusinessException.class,
                () -> interceptor.preHandle(request, response, new Object()));

        assertEquals("AUTH_INVALID_TOKEN", exception.errorCode().name());
    }
}
