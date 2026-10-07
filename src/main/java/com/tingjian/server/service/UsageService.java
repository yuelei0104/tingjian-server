package com.tingjian.server.service;

import com.tingjian.server.dao.UsageDao;
import com.tingjian.server.dto.UsageMetricResponse;
import com.tingjian.server.dto.UsageOverviewResponse;
import com.tingjian.server.dto.UsageResponse;
import com.tingjian.server.service.usage.UsageReservationGateway;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class UsageService {
    private static final long CAPTION_SECONDS_LIMIT = 3_600;
    private static final long TEXT_CHARACTER_LIMIT = 100_000;
    private static final long CONVERSATION_LIMIT = 100;

    private final UsageDao usageDao;
    private final UsageReservationGateway usageGateway;

    public UsageService(UsageDao usageDao, UsageReservationGateway usageGateway) {
        this.usageDao = usageDao;
        this.usageGateway = usageGateway;
    }

    public UsageResponse get(String ownerId) {
        var period = YearMonth.now(ZoneOffset.UTC);
        var periodStart = period.atDay(1).atStartOfDay();
        var periodEndExclusive = period.plusMonths(1).atDay(1).atStartOfDay();
        var summary = usageDao.summary(ownerId, periodStart, periodEndExclusive);

        var metrics = new ArrayList<UsageMetricResponse>();
        metrics.add(metric("CAPTION_SECONDS", "实时字幕", "秒",
                summary.totalDurationSeconds(), CAPTION_SECONDS_LIMIT,
                "按本月已结束会话的持续时间统计"));
        metrics.add(metric("TEXT_CHARACTERS", "保存文字", "字符",
                summary.textCharacterCount(), TEXT_CHARACTER_LIMIT,
                "按本月同步到云端的文字字符数统计"));
        metrics.add(metric("CONVERSATIONS", "会话记录", "段",
                summary.conversationCount(), CONVERSATION_LIMIT,
                "按本月创建的云端会话数统计"));

        var cloudUsage = usageGateway.summary(ownerId);
        cloudUsage.ifPresent(snapshot -> {
            addCloudMetric(metrics, snapshot, UsageReservationGateway.Metric.ASR_SECONDS,
                    "云端语音识别", "秒");
            addCloudMetric(metrics, snapshot, UsageReservationGateway.Metric.AI_REQUESTS,
                    "AI 表达助手", "次");
            addCloudMetric(metrics, snapshot, UsageReservationGateway.Metric.TTS_CHARACTERS,
                    "云端语音播报", "字符");
        });

        String planCode = cloudUsage.map(UsageReservationGateway.UsageSnapshot::planCode)
                .orElse("V1_BETA");
        String planName = switch (planCode) {
            case "PRO" -> "专业版";
            case "FREE" -> "免费版";
            default -> "V1 内测";
        };

        return new UsageResponse(
                planCode,
                planName,
                cloudUsage.isPresent()
                        ? "云端额度每日重置，本机功能不受云端额度影响"
                        : "当前版本不收费，也不会自动购买或续费",
                false,
                period.atDay(1),
                period.atEndOfMonth(),
                new UsageOverviewResponse(
                        summary.conversationCount(),
                        summary.messageCount(),
                        summary.textCharacterCount(),
                        summary.totalDurationSeconds()),
                List.copyOf(metrics));
    }

    private static UsageMetricResponse metric(
            String code, String name, String unit, long used, long limit, String description) {
        var safeUsed = Math.max(0, used);
        var remaining = Math.max(0, limit - safeUsed);
        var progress = limit == 0 ? 0D : Math.min(1D, safeUsed / (double) limit);
        return new UsageMetricResponse(
                code, name, unit, safeUsed, limit, remaining, progress, description);
    }

    private static void addCloudMetric(
            List<UsageMetricResponse> target,
            UsageReservationGateway.UsageSnapshot snapshot,
            UsageReservationGateway.Metric metric,
            String name,
            String unit) {
        UsageReservationGateway.MetricUsage value = snapshot.metrics().get(metric);
        if (value == null) return;
        long consumedOrReserved = Math.max(0, value.limit() - value.remaining());
        double progress = value.limit() == 0
                ? 0D
                : Math.min(1D, consumedOrReserved / (double) value.limit());
        target.add(new UsageMetricResponse(
                metric.name(), name, unit,
                Math.max(0, value.used()), Math.max(0, value.limit()),
                Math.max(0, value.remaining()), progress,
                value.reserved() > 0
                        ? "今日已用；另有 " + value.reserved() + " " + unit + " 正在处理中"
                        : "今日云端用量，次日自动重置"));
    }
}
