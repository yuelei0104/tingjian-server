package com.tingjian.aispeech;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class AgentToolRegistry {
    public ToolSelection select(String message, AgentToolSnapshot snapshot) {
        String normalized = message.toLowerCase(Locale.ROOT);
        List<String> tools = new ArrayList<>();
        StringBuilder context = new StringBuilder();

        if (containsAny(normalized, "术语", "专业词", "glossary", "term")) {
            tools.add("lookupGlossary");
            appendGlossary(context, snapshot.glossary());
        }
        if (containsAny(normalized, "常用语", "快捷回复", "怎么回复", "quick phrase")) {
            tools.add("searchQuickPhrases");
            appendQuickPhrases(context, snapshot.quickPhrases());
        }
        if (containsAny(normalized, "历史", "以前", "上次", "之前", "history")) {
            tools.add("searchHistory");
            appendHistory(context, snapshot.recentHistory());
        }
        if (containsAny(normalized, "额度", "用量", "剩余", "套餐", "quota", "usage")) {
            tools.add("getUsage");
            appendUsage(context, snapshot.usage());
        }
        return new ToolSelection(List.copyOf(tools), context.toString().strip());
    }

    private static boolean containsAny(String source, String... values) {
        for (String value : values) if (source.contains(value)) return true;
        return false;
    }

    private static void appendGlossary(
            StringBuilder target, List<AgentToolSnapshot.GlossaryItem> items) {
        target.append("\n[术语表工具结果]\n");
        items.stream().limit(20).forEach(item -> target.append("- ")
                .append(AgentText.sanitize(item.term(), 80))
                .append(item.alias() == null || item.alias().isBlank()
                        ? "" : " = " + AgentText.sanitize(item.alias(), 80))
                .append("\n"));
    }

    private static void appendQuickPhrases(
            StringBuilder target, List<AgentToolSnapshot.QuickPhraseItem> items) {
        target.append("\n[常用语工具结果]\n");
        items.stream().limit(20).forEach(item -> target.append("- ")
                .append(AgentText.sanitize(item.content(), 160)).append("\n"));
    }

    private static void appendHistory(
            StringBuilder target, List<AgentToolSnapshot.HistoryItem> items) {
        target.append("\n[最近历史工具结果]\n");
        items.stream().limit(5).forEach(item -> target.append("- ")
                .append(AgentText.sanitize(item.title(), 80)).append("：")
                .append(AgentText.sanitize(item.preview(), 180)).append("\n"));
    }

    private static void appendUsage(StringBuilder target, AgentToolSnapshot.UsageItem usage) {
        target.append("\n[用量工具结果]\n");
        if (usage == null) {
            target.append("暂时无法获取云端用量。\n");
            return;
        }
        target.append("套餐：").append(AgentText.sanitize(usage.planName(), 40)).append("\n");
        usage.metrics().stream().limit(10).forEach(metric -> target.append("- ")
                .append(metric.code()).append(" 已用 ").append(metric.used())
                .append("，上限 ").append(metric.limit())
                .append("，剩余 ").append(metric.remaining()).append("\n"));
    }

    public record ToolSelection(List<String> toolsUsed, String renderedContext) {
    }
}
