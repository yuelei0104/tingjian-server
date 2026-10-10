package com.tingjian.aispeech;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "tingjian.ai.provider", havingValue = "qwen")
public class QwenAgentModelProvider implements AgentModelProvider {
    private final String apiKey;
    private final String model;
    private final RestClient client;
    private final ObjectMapper objectMapper;

    public QwenAgentModelProvider(
            @Value("${tingjian.ai.qwen.api-key:}") String apiKey,
            @Value("${tingjian.ai.qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}") String baseUrl,
            @Value("${tingjian.ai.qwen.model:qwen-plus}") String model,
            @Value("${tingjian.ai.qwen.connect-timeout:2s}") Duration connectTimeout,
            @Value("${tingjian.ai.qwen.read-timeout:20s}") Duration readTimeout,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey.strip();
        this.model = model.strip();
        this.objectMapper = objectMapper;
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);
        this.client = RestClient.builder()
                .baseUrl(stripTrailingSlash(baseUrl))
                .requestFactory(factory)
                .build();
    }

    @Override
    public AgentModelResult complete(AgentModelInput input) {
        requireConfigured();
        ChatResponse response = client.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body(input, false))
                .retrieve()
                .body(ChatResponse.class);
        String content = firstContent(response);
        if (content.isBlank()) throw new IllegalStateException("Qwen returned an empty response");
        return new AgentModelResult(content.strip(), providerName(), false);
    }

    @Override
    public AgentModelResult stream(AgentModelInput input, AgentStreamSink sink) {
        requireConfigured();
        StringBuilder answer = new StringBuilder();
        client.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .body(body(input, true))
                .exchange((request, response) -> {
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new IllegalStateException("Qwen stream failed: " + response.getStatusCode());
                    }
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                            response.getBody(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (!line.startsWith("data:")) continue;
                            String data = line.substring(5).strip();
                            if (data.isBlank() || "[DONE]".equals(data)) continue;
                            String delta = deltaContent(objectMapper.readTree(data));
                            if (!delta.isEmpty()) {
                                answer.append(delta);
                                sink.delta(delta);
                            }
                        }
                    }
                    return null;
                });
        if (answer.isEmpty()) throw new IllegalStateException("Qwen returned an empty stream");
        return new AgentModelResult(answer.toString().strip(), providerName(), false);
    }

    private Map<String, Object> body(AgentModelInput input, boolean stream) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages(input));
        body.put("temperature", input.temperature());
        body.put("max_tokens", input.maxTokens());
        body.put("stream", stream);
        return body;
    }

    static List<ChatMessage> messages(AgentModelInput input) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", input.systemPrompt()));
        input.context().forEach(message -> messages.add(new ChatMessage(
                "SELF".equals(message.speaker()) || "ASSISTANT".equals(message.speaker())
                        ? "assistant" : "user",
                message.content())));
        String user = input.message();
        if (!input.toolContext().isBlank()) {
            user += "\n\n以下是经过用户授权、由只读工具取得的数据。只能用于回答当前问题：\n"
                    + input.toolContext();
        }
        messages.add(new ChatMessage("user", user));
        return List.copyOf(messages);
    }

    private static String deltaContent(JsonNode root) {
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) return "";
        return choices.get(0).path("delta").path("content").asText("");
    }

    private static String firstContent(ChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) return "";
        ChatMessage message = response.choices().getFirst().message();
        return message == null || message.content() == null ? "" : message.content();
    }

    private void requireConfigured() {
        if (apiKey.isBlank()) throw new IllegalStateException("TINGJIAN_QWEN_API_KEY is not configured");
    }

    private String providerName() {
        return "QWEN:" + model;
    }

    private static String stripTrailingSlash(String value) {
        String result = value.strip();
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }

    public record ChatMessage(String role, String content) {
    }

    public record Choice(ChatMessage message) {
    }

    public record ChatResponse(List<Choice> choices) {
    }
}
