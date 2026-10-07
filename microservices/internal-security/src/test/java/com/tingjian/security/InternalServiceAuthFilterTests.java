package com.tingjian.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class InternalServiceAuthFilterTests {
    private final InternalServiceAuthFilter filter = new InternalServiceAuthFilter("test-secret");

    @Test
    void allowsInternalRequestWithMatchingToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/usage/reservations");
        request.addHeader(InternalServiceAuthFilter.HEADER_NAME, "test-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean called = new AtomicBoolean();
        FilterChain chain = (ignoredRequest, ignoredResponse) -> called.set(true);

        filter.doFilter(request, response, chain);

        assertThat(called).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsMissingTokenWithStableErrorEnvelope() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/ai/status");
        request.addHeader("X-Request-Id", "request-1234");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean called = new AtomicBoolean();

        filter.doFilter(request, response,
                (ignoredRequest, ignoredResponse) -> called.set(true));

        assertThat(called).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                .contains("INTERNAL_AUTH_FAILED", "request-1234");
    }

    @Test
    void ignoresPublicAndHealthEndpoints() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean called = new AtomicBoolean();

        filter.doFilter(request, response,
                (ignoredRequest, ignoredResponse) -> called.set(true));

        assertThat(called).isTrue();
    }
}
