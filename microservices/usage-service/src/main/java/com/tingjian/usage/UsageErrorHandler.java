package com.tingjian.usage;

import com.tingjian.contract.ApiEnvelope;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class UsageErrorHandler {
    @ExceptionHandler(UsageException.class)
    ResponseEntity<ApiEnvelope<Void>> handle(
            UsageException exception,
            HttpServletRequest request) {
        HttpStatus status = switch (exception.code()) {
            case "RESERVATION_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "IDEMPOTENCY_CONFLICT", "INVALID_RESERVATION_STATE" -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status)
                .body(ApiEnvelope.failure(
                        exception.code(), exception.getMessage(), request.getHeader("X-Request-Id")));
    }
}
