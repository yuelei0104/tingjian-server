package com.tingjian.aispeech;

import com.tingjian.contract.ApiEnvelope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/ai")
public class ProviderStatusController {
    private final ProviderStatusService service;

    public ProviderStatusController(ProviderStatusService service) {
        this.service = service;
    }

    @GetMapping("/status")
    public ApiEnvelope<ProviderStatusResponse> status(
            @RequestHeader(name = "X-Request-Id", required = false) String requestId) {
        return ApiEnvelope.success(service.current(), requestId);
    }
}
