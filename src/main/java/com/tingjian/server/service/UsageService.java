package com.tingjian.server.service;

import com.tingjian.server.dao.UsageDao;
import com.tingjian.server.dto.UsageMetricResponse;
import com.tingjian.server.dto.UsageOverviewResponse;
import com.tingjian.server.dto.UsageResponse;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class UsageService {
    private static final long CAPTION_SECONDS_LIMIT = 3_600;
    private static final long TEXT_CHARACTER_LIMIT = 100_000;
    private static final long CONVERSATION_LIMIT = 100;

    private final UsageDao usageDao;

    public UsageService(UsageDao usageDao) {
        this.usageDao = usageDao;
    }

    public UsageResponse get(String ownerId) {
        var period = YearMonth.now(ZoneOffset.UTC);
        var periodStart = period.atDay(1).atStartOfDay();
        var periodEndExclusive = period.plusMonths(1).atDay(1).atStartOfDay();
        var summary = usageDao.summary(ownerId, periodStart, periodEndExclusive);

        var metrics = List.of(
                metric("CAPTION_SECONDS", "实时字幕", "秒",
                        summary.totalDurationSeconds(), CAPTION_SECONDS_LIMIT,
                        "按本月已结束会话的持续时间统计"),
                metric("TEXT_CHARACTERS", "保存文字", "字符",
                        summary.textCharacterCount(), TEXT_CHARACTER_LIMIT,
                        "按本月同步到云端的文字字符数统计"),
                metric("CONVERSATIONS", "会话记录", "段",
                        summary.conversationCount(), CONVERSATION_LIMIT,
                        "按本月创建的云端会话数统计"));

        return new UsageResponse(
                "V1_BETA",
                "V1 内测",
                "当前版本不收费，也不会自动购买或续费",
                false,
                period.atDay(1),
                period.atEndOfMonth(),
                new UsageOverviewResponse(
                        summary.conversationCount(),
                        summary.messageCount(),
                        summary.textCharacterCount(),
                        summary.totalDurationSeconds()),
                metrics);
    }

    private static UsageMetricResponse metric(
            String code, String name, String unit, long used, long limit, String description) {
        var safeUsed = Math.max(0, used);
        var remaining = Math.max(0, limit - safeUsed);
        var progress = limit == 0 ? 0D : Math.min(1D, safeUsed / (double) limit);
        return new UsageMetricResponse(
                code, name, unit, safeUsed, limit, remaining, progress, description);
    }
}
