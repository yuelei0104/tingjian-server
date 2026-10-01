package com.tingjian.server.config;

import com.tingjian.server.common.BusinessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class RealtimeHandshakeInterceptor implements HandshakeInterceptor {
    public static final String USER_ID_ATTRIBUTE =
            RealtimeHandshakeInterceptor.class.getName() + ".userId";

    private final AccessTokenService accessTokenService;

    public RealtimeHandshakeInterceptor(AccessTokenService accessTokenService) {
        this.accessTokenService = accessTokenService;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        try {
            String userId = accessTokenService.resolveUserId(
                    request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
            attributes.put(USER_ID_ATTRIBUTE, userId);
            return true;
        } catch (BusinessException ignored) {
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // No handshake resources to release.
    }
}
