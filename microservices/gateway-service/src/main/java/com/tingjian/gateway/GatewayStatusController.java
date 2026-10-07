package com.tingjian.gateway;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class GatewayStatusController {
    @GetMapping("/gateway/status")
    GatewayStatusResponse status() {
        return new GatewayStatusResponse(
                "UP", "tingjian-gateway", true, List.of("/api/**", "/ws/**"));
    }

    record GatewayStatusResponse(
            String status,
            String service,
            boolean authenticationBoundary,
            List<String> publicRoutes) {
    }
}
