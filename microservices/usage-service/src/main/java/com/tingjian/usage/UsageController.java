package com.tingjian.usage;

import com.tingjian.contract.ApiEnvelope;
import com.tingjian.contract.PlanTier;
import com.tingjian.contract.UsageReservationRequest;
import com.tingjian.contract.UsageReservationResponse;
import com.tingjian.contract.UsageSummaryResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/usage")
public class UsageController {
    private final UsageQuotaService service;

    public UsageController(UsageQuotaService service) {
        this.service = service;
    }

    @GetMapping("/users/{userId}")
    public ApiEnvelope<UsageSummaryResponse> summary(
            @PathVariable String userId,
            @RequestHeader(name = "X-Request-Id", required = false) String requestId) {
        return ApiEnvelope.success(service.summary(userId), requestId);
    }

    @PutMapping("/users/{userId}/plan/{plan}")
    public ApiEnvelope<UsageSummaryResponse> setPlan(
            @PathVariable String userId,
            @PathVariable PlanTier plan,
            @RequestHeader(name = "X-Request-Id", required = false) String requestId) {
        return ApiEnvelope.success(service.setPlan(userId, plan), requestId);
    }

    @PostMapping("/reservations")
    public ApiEnvelope<UsageReservationResponse> reserve(
            @RequestBody UsageReservationRequest request,
            @RequestHeader(name = "X-Request-Id", required = false) String requestId) {
        return ApiEnvelope.success(service.reserve(request), requestId);
    }

    @PostMapping("/reservations/{reservationId}/commit")
    public ApiEnvelope<UsageReservationResponse> commit(
            @PathVariable String reservationId,
            @RequestHeader(name = "X-Request-Id", required = false) String requestId) {
        return ApiEnvelope.success(service.commit(reservationId), requestId);
    }

    @DeleteMapping("/reservations/{reservationId}")
    public ApiEnvelope<UsageReservationResponse> release(
            @PathVariable String reservationId,
            @RequestHeader(name = "X-Request-Id", required = false) String requestId) {
        return ApiEnvelope.success(service.release(reservationId), requestId);
    }
}
