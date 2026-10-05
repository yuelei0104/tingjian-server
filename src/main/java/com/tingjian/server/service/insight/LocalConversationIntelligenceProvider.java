package com.tingjian.server.service.insight;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class LocalConversationIntelligenceProvider implements ConversationIntelligenceProvider {
    private static final Pattern ACTION_PATTERN = Pattern.compile(
            "(?i).*(请|需要|记得|安排|提交|完成|确认|提醒|明天|后天|下周|todo|must|need to|please).*"
    );
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{IsHan}]{2,8}|[A-Za-z]{4,}");
    private static final Set<String> STOP_WORDS = Set.of(
            "好的", "可以", "这个", "那个", "我们", "你们", "他们", "已经", "然后",
            "就是", "还是", "进行", "需要", "please", "that", "this", "with", "have");

    @Override
    public ConversationInsightResult analyze(ConversationInsightInput input) {
        return analyzeLocally(input, "LOCAL_INSIGHT_V1");
    }

    public static ConversationInsightResult fallback(ConversationInsightInput input) {
        return analyzeLocally(input, "LOCAL_INSIGHT_FALLBACK");
    }

    private static ConversationInsightResult analyzeLocally(
            ConversationInsightInput input, String generatedBy) {
        List<String> contents = input.messages().stream()
                .map(ConversationInsightMessage::content)
                .map(String::strip)
                .filter(content -> !content.isBlank())
                .toList();
        if (contents.isEmpty()) {
            return new ConversationInsightResult(
                    "这段会话暂时没有可供整理的文字。",
                    List.of(), List.of(), List.of(), "暂无", generatedBy);
        }

        List<String> highlights = new ArrayList<>(new LinkedHashSet<>(contents)).stream()
                .map(content -> clip(content, 100))
                .limit(3)
                .toList();
        List<String> actionItems = contents.stream()
                .filter(content -> ACTION_PATTERN.matcher(content).matches())
                .map(content -> clip(content, 120))
                .distinct()
                .limit(5)
                .toList();
        List<String> keywords = extractKeywords(contents);
        String tone = tone(contents);
        String summary = "会话共 " + contents.size() + " 条文字。主要内容："
                + String.join("；", highlights) + "。"
                + (actionItems.isEmpty() ? "" : "识别到 " + actionItems.size() + " 项待办。")
                + (keywords.isEmpty() ? "" : "关键词：" + String.join("、", keywords) + "。");
        return new ConversationInsightResult(
                summary, highlights, actionItems, keywords, tone, generatedBy);
    }

    private static List<String> extractKeywords(List<String> contents) {
        Map<String, Long> counts = contents.stream()
                .flatMap(content -> TOKEN_PATTERN.matcher(content).results()
                        .map(match -> match.group().toLowerCase(Locale.ROOT)))
                .filter(token -> !STOP_WORDS.contains(token))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .limit(5)
                .toList();
    }

    private static String tone(List<String> contents) {
        String joined = String.join(" ", contents).toLowerCase(Locale.ROOT);
        if (List.of("谢谢", "很好", "可以", "成功", "完成", "great", "thanks")
                .stream().anyMatch(joined::contains)) return "积极";
        if (List.of("失败", "错误", "担心", "无法", "问题", "抱歉", "error", "failed")
                .stream().anyMatch(joined::contains)) return "需关注";
        long questions = contents.stream()
                .filter(content -> content.contains("?") || content.contains("？"))
                .count();
        return questions * 2 >= contents.size() ? "待确认" : "平稳";
    }

    private static String clip(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "…";
    }
}
