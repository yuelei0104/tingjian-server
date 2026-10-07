package com.tingjian.gateway;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayStatusControllerTests {
    @Test
    void exposesOnlyPublicGatewayRoutes() {
        GatewayStatusController.GatewayStatusResponse status =
                new GatewayStatusController().status();

        assertThat(status.status()).isEqualTo("UP");
        assertThat(status.authenticationBoundary()).isTrue();
        assertThat(status.publicRoutes()).containsExactly("/api/**", "/ws/**");
    }
}
