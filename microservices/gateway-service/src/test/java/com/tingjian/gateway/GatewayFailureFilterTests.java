package com.tingjian.gateway;

import org.junit.jupiter.api.Test;

import java.net.ConnectException;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayFailureFilterTests {
    @Test
    void recognizesNestedDownstreamConnectionFailure() {
        RuntimeException wrapped = new RuntimeException("route failed",
                new ConnectException("connection refused"));

        assertThat(GatewayFailureFilter.isDownstreamUnavailable(wrapped)).isTrue();
        assertThat(GatewayFailureFilter.isDownstreamUnavailable(
                new IllegalArgumentException("bad request"))).isFalse();
    }
}
