package com.tingjian.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.UnresolvedAddressException;
import java.util.concurrent.TimeoutException;

@Component
public class GatewayFailureFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).onErrorResume(error -> {
            if (!isDownstreamUnavailable(error) || exchange.getResponse().isCommitted()) {
                return Mono.error(error);
            }
            return GatewayErrorWriter.write(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                    "DOWNSTREAM_UNAVAILABLE", "服务暂时不可用，请稍后重试");
        });
    }

    static boolean isDownstreamUnavailable(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof ConnectException
                    || current instanceof SocketTimeoutException
                    || current instanceof UnknownHostException
                    || current instanceof UnresolvedAddressException
                    || current instanceof TimeoutException) {
                return true;
            }
            String name = current.getClass().getSimpleName();
            if (name.contains("ConnectTimeout") || name.contains("ReadTimeout")) return true;
            current = current.getCause();
        }
        return false;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
