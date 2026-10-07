package com.tingjian.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayAuthenticationFilterTests {
    private final GatewayAuthenticationFilter filter = new GatewayAuthenticationFilter();

    @Test
    void recognizesProtectedRoutesAndBearerTokens() {
        assertThat(GatewayAuthenticationFilter.isProtectedPath("/api/v1/history")).isTrue();
        assertThat(GatewayAuthenticationFilter.isProtectedPath("/api/v1")).isTrue();
        assertThat(GatewayAuthenticationFilter.isProtectedPath("/ws/v1/messages")).isTrue();
        assertThat(GatewayAuthenticationFilter.isProtectedPath("/api/auth/login")).isFalse();
        assertThat(GatewayAuthenticationFilter.hasBearerToken("Bearer token-value")).isTrue();
        assertThat(GatewayAuthenticationFilter.hasBearerToken("Bearer  ")).isFalse();
    }

    @Test
    void rejectsProtectedRequestWithoutToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/history").build());
        AtomicBoolean called = new AtomicBoolean();

        filter.filter(exchange, ignored -> {
            called.set(true);
            return reactor.core.publisher.Mono.empty();
        }).block();

        assertThat(called).isFalse();
        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void forwardsAuthorizedRequestAndRemovesSpoofedInternalToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-value")
                        .header(GatewayAuthenticationFilter.INTERNAL_TOKEN_HEADER, "spoofed")
                        .build());
        AtomicBoolean tokenWasRemoved = new AtomicBoolean();

        filter.filter(exchange, forwarded -> {
            tokenWasRemoved.set(forwarded.getRequest().getHeaders()
                    .getFirst(GatewayAuthenticationFilter.INTERNAL_TOKEN_HEADER)==null);
            return reactor.core.publisher.Mono.empty();
        }).block();

        assertThat(tokenWasRemoved).isTrue();
    }
}
