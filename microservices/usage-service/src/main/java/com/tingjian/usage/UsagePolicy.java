package com.tingjian.usage;

import com.tingjian.contract.PlanTier;
import com.tingjian.contract.UsageMetric;

import java.util.EnumMap;
import java.util.Map;

final class UsagePolicy {
    private static final Map<PlanTier, Map<UsageMetric, Long>> LIMITS = Map.of(
            PlanTier.FREE, limits(1_800, 100, 10_000),
            PlanTier.PRO, limits(14_400, 1_000, 200_000)
    );

    private UsagePolicy() {
    }

    static long limit(PlanTier plan, UsageMetric metric) {
        return LIMITS.get(plan).get(metric);
    }

    private static Map<UsageMetric, Long> limits(long asrSeconds, long aiRequests, long ttsCharacters) {
        EnumMap<UsageMetric, Long> result = new EnumMap<>(UsageMetric.class);
        result.put(UsageMetric.ASR_SECONDS, asrSeconds);
        result.put(UsageMetric.AI_REQUESTS, aiRequests);
        result.put(UsageMetric.TTS_CHARACTERS, ttsCharacters);
        return Map.copyOf(result);
    }
}
