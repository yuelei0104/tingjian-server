package com.tingjian.server.config;

import com.tingjian.server.service.RealtimeWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class RealtimeWebSocketConfig implements WebSocketConfigurer {
    private final RealtimeWebSocketHandler handler;
    private final RealtimeHandshakeInterceptor handshakeInterceptor;
    private final CorsProperties corsProperties;

    public RealtimeWebSocketConfig(
            RealtimeWebSocketHandler handler,
            RealtimeHandshakeInterceptor handshakeInterceptor,
            CorsProperties corsProperties) {
        this.handler = handler;
        this.handshakeInterceptor = handshakeInterceptor;
        this.corsProperties = corsProperties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/realtime")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns(corsProperties.allowedOriginPatterns());
    }
}
