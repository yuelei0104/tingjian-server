package com.tingjian.server.config;

import com.tingjian.server.common.RequestTrace;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTraceFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestTraceFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String requestId = RequestTrace.resolve(request.getHeader(RequestTrace.HEADER_NAME));
        long startedAt = System.nanoTime();
        RequestTrace.set(requestId);
        MDC.put(RequestTrace.MDC_KEY, requestId);
        response.setHeader(RequestTrace.HEADER_NAME, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            log.info("HTTP {} {} -> {} ({} ms)", request.getMethod(),
                    request.getRequestURI(), response.getStatus(), elapsedMs);
            MDC.remove(RequestTrace.MDC_KEY);
            RequestTrace.clear();
        }
    }
}
