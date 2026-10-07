package com.tingjian.server.service.usage;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.tingjian.server.common.BusinessException;
import com.tingjian.server.common.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpUsageReservationGatewayTests {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void reservesCommitsAndReleasesThroughHttp() throws Exception {
        AtomicInteger commits = new AtomicInteger();
        AtomicInteger releases = new AtomicInteger();
        AtomicInteger authenticatedRequests = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/usage/reservations", exchange -> {
            if ("test-internal-token".equals(
                    exchange.getRequestHeaders().getFirst(HttpUsageReservationGateway.INTERNAL_TOKEN_HEADER))) {
                authenticatedRequests.incrementAndGet();
            }
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("/commit")) {
                commits.incrementAndGet();
                respond(exchange, 200, "");
            } else if ("DELETE".equals(exchange.getRequestMethod())) {
                releases.incrementAndGet();
                respond(exchange, 200, "");
            } else {
                respond(exchange, 200, envelope("RESERVED"));
            }
        });
        server.start();
        HttpUsageReservationGateway gateway = gateway(false);

        UsageReservationGateway.Reservation reservation = gateway.reserve(
                "user-1", UsageReservationGateway.Metric.AI_REQUESTS, 1, "ai:request-1");
        gateway.commit(reservation);
        gateway.release(reservation);

        assertTrue(reservation.metered());
        assertEquals("reservation-1", reservation.id());
        assertEquals(1, commits.get());
        assertEquals(1, releases.get());
        assertEquals(3, authenticatedRequests.get());
    }

    @Test
    void translatesRejectedReservationToQuotaError() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/usage/reservations",
                exchange -> respond(exchange, 200, envelope("REJECTED")));
        server.start();
        HttpUsageReservationGateway gateway = gateway(false);

        BusinessException error = assertThrows(BusinessException.class, () -> gateway.reserve(
                "user-1", UsageReservationGateway.Metric.AI_REQUESTS, 1, "ai:request-1"));

        assertEquals(ErrorCode.USAGE_QUOTA_EXCEEDED, error.errorCode());
    }

    @Test
    void failOpenAllowsRequestWhenUsageServiceIsUnavailable() {
        HttpUsageReservationGateway gateway = new HttpUsageReservationGateway(
                "http://127.0.0.1:1", Duration.ofMillis(100), Duration.ofMillis(100),
                "test-internal-token", true);

        UsageReservationGateway.Reservation reservation = gateway.reserve(
                "user-1", UsageReservationGateway.Metric.AI_REQUESTS, 1, "ai:request-1");

        assertTrue(!reservation.metered());
    }

    private HttpUsageReservationGateway gateway(boolean failOpen) {
        return new HttpUsageReservationGateway(
                "http://127.0.0.1:" + server.getAddress().getPort(),
                Duration.ofSeconds(1), Duration.ofSeconds(1),
                "test-internal-token", failOpen);
    }

    private static String envelope(String status) {
        return """
                {"success":true,"data":{"reservationId":"reservation-1","status":"%s",
                "metric":"AI_REQUESTS","amount":1,"remaining":99,"resetDate":"2026-10-07"},
                "error":null,"requestId":"test-request"}
                """.formatted(status);
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
