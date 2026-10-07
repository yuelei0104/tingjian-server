package com.tingjian.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GatewayAuthenticationFilter implements GlobalFilter, Ordered {
    static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (!HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())
                && isProtectedPath(path)
                && !hasBearerToken(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))) {
            return GatewayErrorWriter.write(exchange, HttpStatus.UNAUTHORIZED,
                    "AUTH_REQUIRED", "请先登录后再访问此功能");
        }

        ServerHttpRequest sanitizedRequest = exchange.getRequest().mutate()
                .headers(headers -> headers.remove(INTERNAL_TOKEN_HEADER))
                .build();
        return chain.filter(exchange.mutate().request(sanitizedRequest).build());
    }

    static boolean isProtectedPath(String path) {
        return path != null && (matchesPathPrefix(path, "/api/v1")
                || matchesPathPrefix(path, "/ws"));
    }

    private static boolean matchesPathPrefix(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    static boolean hasBearerToken(String authorization) {
        if (authorization == null) return false;
        int separator = authorization.indexOf(' ');
        return separator > 0
                && authorization.substring(0, separator).equalsIgnoreCase("Bearer")
                && !authorization.substring(separator + 1).isBlank();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
