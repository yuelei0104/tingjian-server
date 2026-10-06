package com.tingjian.server.service.ai;

import com.tingjian.server.config.QwenProperties;
import com.tingjian.server.dto.AiContextMessageRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "tingjian.ai.provider", havingValue = "qwen")
public class QwenAiExpressionProvider implements AiExpressionProvider {
    private final QwenProperties properties;
    private final RestClient client;

    public QwenAiExpressionProvider(QwenProperties properties) {
        this.properties = properties;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        this.client = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public AiProviderResult suggest(AiProviderInput input) {
        if (properties.apiKey().isBlank()) {
            throw new IllegalStateException("DASHSCOPE_API_KEY is not configured");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.model());
        body.put("messages", messages(input));
        body.put("temperature", 0.25);
        body.put("max_tokens", 500);

        ChatResponse response = client.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(ChatResponse.class);
        String content = firstContent(response);
        if (content.isBlank()) throw new IllegalStateException("Qwen returned an empty response");
        return new AiProviderResult(content.strip(), "QWEN:" + properties.model(), false);
    }

    static List<ChatMessage> messages(AiProviderInput input) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", systemPrompt(input)));
        for (AiContextMessageRequest message : input.context()) {
            messages.add(new ChatMessage(
                    "SELF".equals(message.speaker()) ? "assistant" : "user",
                    message.content()));
        }
        messages.add(new ChatMessage("user", userPrompt(input)));
        return List.copyOf(messages);
    }

    private static String systemPrompt(AiProviderInput input) {
        return "你是听见App的无障碍沟通助手。仅输出可直接展示或发送的文本，不解释过程，不使用Markdown。"
                + "不得编造事实，不得输出隐私或密钥。输出语言：" + input.language() + "。";
    }

    private static String userPrompt(AiProviderInput input) {
        String instruction = switch (input.action()) {
            case REPLY -> "根据对话上下文生成一条自然、简短、礼貌的回复。";
            case POLITE -> "将下面文字改写得更礼貌，含义保持不变。";
            case CONCISE -> "将下面文字压缩得更简洁，保留关键信息。";
            case FORMAL -> "将下面文字改写为正式表达，含义保持不变。";
            case TRANSLATE_ZH -> "将下面文字翻译为自然中文。";
            case TRANSLATE_EN -> "将下面文字翻译为自然英文。";
        };
        return instruction + (input.sourceText().isBlank() ? "" : "\n待处理文字：" + input.sourceText());
    }

    private static String firstContent(ChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) return "";
        ChatMessage message = response.choices().getFirst().message();
        return message == null || message.content() == null ? "" : message.content();
    }

    public record ChatMessage(String role, String content) {
    }

    public record Choice(ChatMessage message) {
    }

    public record ChatResponse(List<Choice> choices) {
    }
}
