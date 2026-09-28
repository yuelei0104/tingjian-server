package com.tingjian.server.dto;

import java.time.LocalDate;
import java.util.List;

public record UsageResponse(
        String planCode,
        String planName,
        String planDescription,
        boolean purchasable,
        LocalDate periodStart,
        LocalDate periodEnd,
        UsageOverviewResponse overview,
        List<UsageMetricResponse> metrics) {
}
