package com.tingjian.aispeech;


import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

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

@Service
public class ConversationInsightService {
    private static final Pattern ACTION_PATTERN = Pattern.compile(
            "(?i).*(请|需要|记得|安排|提交|完成|确认|提醒|明天|后天|下周|todo|must|need to|please).*"
    );
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{IsHan}]{2,8}|[A-Za-z]{4,}");
    private static final Set<String> STOP_WORDS = Set.of(
            "好的", "可以", "这个", "那个", "我们", "你们", "他们", "已经", "然后",
            "就是", "还是", "进行", "需要", "please", "that", "this", "with", "have");
    private static final String SYSTEM_PROMPT = """
            你是听见App的会话整理助手。请根据会话生成严格 JSON，不要使用 Markdown 代码块。
            JSON字段必须为 summary、highlights、actionItems、keywords、tone。
            highlights最多3项，actionItems最多5项，keywords最多5项；tone使用简短中文。
            不得编造会话中没有的事实、姓名、时间或任务。
            """;

    private final AgentModelProvider provider;
    private final ObjectMapper objectMapper;

    public ConversationInsightService(AgentModelProvider provider, ObjectMapper objectMapper) {
        this.provider = provider;
        this.objectMapper = objectMapper;
    }

    public ConversationInsightResponse analyze(ConversationInsightRequest request) {
        List<AgentContextMessage> messages = AgentText.context(request.messages());
        if (messages.isEmpty()) return local(messages, "LOCAL_INSIGHT_EMPTY", true);
        String transcript = messages.stream()
                .map(message -> message.speaker() + ": " + message.content())
                .collect(Collectors.joining("\n"));
        AgentModelInput input = new AgentModelInput(
                SYSTEM_PROMPT,
                "请整理下面的会话：\n" + transcript,
                List.of(), "", 0.15, 900);
        try {
            AgentModelResult model = provider.complete(input);
            if (model == null || model.fallback()) return local(messages, "LOCAL_INSIGHT_FALLBACK", true);
            InsightPayload payload = objectMapper.readValue(stripCodeFence(model.text()), InsightPayload.class);
            if (payload.summary() == null || payload.summary().isBlank()) {
                return local(messages, "LOCAL_INSIGHT_FALLBACK", true);
            }
            return new ConversationInsightResponse(
                    AgentText.sanitize(payload.summary(), 1000),
                    sanitizeList(payload.highlights(), 3, 200),
                    sanitizeList(payload.actionItems(), 5, 200),
                    sanitizeList(payload.keywords(), 5, 50),
                    AgentText.sanitize(payload.tone(), 40), model.provider(), false);
        } catch (RuntimeException exception) {
            return local(messages, "LOCAL_INSIGHT_FALLBACK", true);
        }
    }

    private static ConversationInsightResponse local(
            List<AgentContextMessage> messages, String generatedBy, boolean fallback) {
        List<String> contents = messages.stream().map(AgentContextMessage::content).toList();
        if (contents.isEmpty()) {
            return new ConversationInsightResponse(
                    "这段会话暂时没有可供整理的文字。",
                    List.of(), List.of(), List.of(), "暂无", generatedBy, fallback);
        }
        List<String> highlights = new ArrayList<>(new LinkedHashSet<>(contents)).stream()
                .map(content -> AgentText.sanitize(content, 100)).limit(3).toList();
        List<String> actions = contents.stream()
                .filter(content -> ACTION_PATTERN.matcher(content).matches())
                .map(content -> AgentText.sanitize(content, 120)).distinct().limit(5).toList();
        List<String> keywords = keywords(contents);
        String summary = "会话共 " + contents.size() + " 条文字。主要内容："
                + String.join("；", highlights) + "。"
                + (actions.isEmpty() ? "" : "识别到 " + actions.size() + " 项待办。")
                + (keywords.isEmpty() ? "" : "关键词：" + String.join("、", keywords) + "。");
        return new ConversationInsightResponse(
                summary, highlights, actions, keywords, tone(contents), generatedBy, fallback);
    }

    private static List<String> sanitizeList(List<String> values, int limit, int length) {
        if (values == null) return List.of();
        return values.stream().map(value -> AgentText.sanitize(value, length))
                .filter(value -> !value.isBlank()).distinct().limit(limit).toList();
    }

    private static List<String> keywords(List<String> contents) {
        Map<String, Long> counts = contents.stream()
                .flatMap(content -> TOKEN_PATTERN.matcher(content).results()
                        .map(match -> match.group().toLowerCase(Locale.ROOT)))
                .filter(token -> !STOP_WORDS.contains(token))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey).limit(5).toList();
    }

    private static String tone(List<String> contents) {
        String joined = String.join(" ", contents).toLowerCase(Locale.ROOT);
        if (List.of("谢谢", "很好", "成功", "完成", "great", "thanks")
                .stream().anyMatch(joined::contains)) return "积极";
        if (List.of("失败", "错误", "担心", "无法", "问题", "抱歉", "error", "failed")
                .stream().anyMatch(joined::contains)) return "需关注";
        long questions = contents.stream()
                .filter(content -> content.contains("?") || content.contains("？")).count();
        return questions * 2 >= contents.size() ? "待确认" : "平稳";
    }

    private static String stripCodeFence(String value) {
        String clean = value == null ? "" : value.strip();
        if (clean.startsWith("```")) {
            int firstBreak = clean.indexOf('\n');
            int lastFence = clean.lastIndexOf("```");
            if (firstBreak >= 0 && lastFence > firstBreak) {
                clean = clean.substring(firstBreak + 1, lastFence).strip();
            }
        }
        return clean;
    }

    private record InsightPayload(
            String summary,
            List<String> highlights,
            List<String> actionItems,
            List<String> keywords,
            String tone) {
    }
}
