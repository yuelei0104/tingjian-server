package com.tingjian.server.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class RequestTraceTests {
    @AfterEach
    void clear() {
        RequestTrace.clear();
    }

    @Test
    void acceptsSafeClientRequestId() {
        assertEquals("android-1234", RequestTrace.resolve(" android-1234 "));
    }

    @Test
    void replacesUnsafeRequestId() {
        assertNotEquals("bad request id", RequestTrace.resolve("bad request id"));
    }

    @Test
    void apiResponseReusesCurrentRequestId() {
        RequestTrace.set("request-1234");
        assertEquals("request-1234", ApiResponse.success("ok").requestId());
    }
}
