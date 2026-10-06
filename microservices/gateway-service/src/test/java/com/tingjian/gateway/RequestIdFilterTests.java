package com.tingjian.gateway;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTests {
    @Test
    void keepsSafeIncomingRequestId() {
        assertThat(RequestIdFilter.normalize("client-request_123"))
                .isEqualTo("client-request_123");
    }

    @Test
    void replacesUnsafeOrMissingRequestId() {
        assertThat(RequestIdFilter.normalize(null)).matches("[0-9a-f-]{36}");
        assertThat(RequestIdFilter.normalize("bad id with spaces")).matches("[0-9a-f-]{36}");
    }
}
