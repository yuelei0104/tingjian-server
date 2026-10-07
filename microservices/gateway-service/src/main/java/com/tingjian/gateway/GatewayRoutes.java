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
            @Value("${tingjian.routes.legacy-websocket}") String legacyWebSocket) {
        return builder.routes()
                .route("legacy-websocket", route -> route
                        .path("/ws/**")
                        .uri(legacyWebSocket))
                .route("legacy-api", route -> route
                        .path("/api/**")
                        .uri(legacyHttp))
                .build();
    }
}
