package com.tingjian.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Pattern;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class InternalServiceAuthFilter extends OncePerRequestFilter {
    public static final String HEADER_NAME = "X-Internal-Service-Token";
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{8,64}");
    private final byte[] expectedToken;

    public InternalServiceAuthFilter(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("internal service token must not be blank");
        }
        this.expectedToken = token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String supplied = request.getHeader(HEADER_NAME);
        if (supplied != null && MessageDigest.isEqual(
                expectedToken, supplied.getBytes(StandardCharsets.UTF_8))) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String requestId = safeRequestId(request.getHeader("X-Request-Id"));
        response.getWriter().write(("{\"success\":false,\"data\":null,\"error\":{"
                + "\"code\":\"INTERNAL_AUTH_FAILED\",\"message\":\"内部服务认证失败\"},"
                + "\"requestId\":\"%s\"}").formatted(requestId));
    }

    private static String safeRequestId(String value) {
        return value != null && SAFE_REQUEST_ID.matcher(value).matches() ? value : "";
    }
}
