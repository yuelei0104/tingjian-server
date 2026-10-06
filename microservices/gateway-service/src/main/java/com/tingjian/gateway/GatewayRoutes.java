package com.tingjian.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutes {
    @Bean
    RouteLocator tingjianRoutes(
            RouteLocatorBuilder builder,
            @Value("${tingjian.routes.legacy-http}") String legacyHttp,
            @Value("${tingjian.routes.legacy-websocket}") String legacyWebSocket,
            @Value("${tingjian.routes.ai-speech}") String aiSpeech,
            @Value("${tingjian.routes.usage}") String usage) {
        return builder.routes()
                .route("ai-speech-service", route -> route
                        .path("/internal/ai/**")
                        .uri(aiSpeech))
                .route("usage-service", route -> route
                        .path("/internal/usage/**")
                        .uri(usage))
                .route("legacy-websocket", route -> route
                        .path("/ws/**")
                        .uri(legacyWebSocket))
                .route("legacy-api", route -> route
                        .path("/api/**")
                        .uri(legacyHttp))
                .build();
    }
}
