package com.tingjian.gateway;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

final class GatewayErrorWriter {
    private GatewayErrorWriter() {
    }

    static Mono<Void> write(
            ServerWebExchange exchange, HttpStatus status, String code, String message) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(new IllegalStateException("gateway response is already committed"));
        }
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String requestId = RequestIdFilter.normalize(
                exchange.getResponse().getHeaders().getFirst(RequestIdFilter.HEADER_NAME));
        exchange.getResponse().getHeaders().set(RequestIdFilter.HEADER_NAME, requestId);
        String json = ("{\"success\":false,\"data\":null,\"error\":{"
                + "\"code\":\"%s\",\"message\":\"%s\"},\"requestId\":\"%s\"}")
                .formatted(code, message, requestId);
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(json.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
