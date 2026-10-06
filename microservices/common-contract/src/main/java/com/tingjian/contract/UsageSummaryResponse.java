package com.tingjian.contract;

import java.time.LocalDate;
import java.util.Map;

public record UsageSummaryResponse(
        String userId,
        PlanTier plan,
        LocalDate resetDate,
        Map<UsageMetric, MetricUsage> metrics
) {
    public record MetricUsage(long used, long reserved, long limit, long remaining) {
    }
}
