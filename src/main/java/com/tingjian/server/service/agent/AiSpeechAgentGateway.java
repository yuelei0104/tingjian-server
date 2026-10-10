package com.tingjian.server.service.agent;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.tingjian.server.config.AiSpeechServiceProperties;
import com.tingjian.server.service.ai.AiProviderInput;
import com.tingjian.server.service.ai.AiProviderResult;
import com.tingjian.server.service.insight.ConversationInsightInput;
import com.tingjian.server.service.insight.ConversationInsightResult;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class AiSpeechAgentGateway {
    static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";

    private final AiSpeechServiceProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient client;

    public AiSpeechAgentGateway(
            AiSpeechServiceProperties properties,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout()).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(properties.readTimeout());
        this.client = RestClient.builder()
                .baseUrl(properties.baseUrl()).requestFactory(factory).build();
    }

    public boolean enabled() {
        return properties.enabled();
    }

    AiSpeechContracts.AgentResponse respond(AiSpeechContracts.AgentRequest request) {
        requireEnabled();
        return request("/internal/ai/agent/respond")
                .body(request).retrieve().body(AiSpeechContracts.AgentResponse.class);
    }

    AiSpeechContracts.AgentResponse stream(
            AiSpeechContracts.AgentRequest request,
            AgentStreamListener listener) {
        requireEnabled();
        AtomicReference<AiSpeechContracts.AgentResponse> completed = new AtomicReference<>();
        request("/internal/ai/agent/stream")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .body(request)
                .exchange((httpRequest, response) -> {
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new IllegalStateException(
                                "ai-speech stream failed: " + response.getStatusCode());
                    }
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                            response.getBody(), StandardCharsets.UTF_8))) {
                        String event = "message";
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("event:")) {
                                event = line.substring(6).strip();
                            } else if (line.startsWith("data:")) {
                                handleEvent(event, line.substring(5).strip(), listener, completed);
                                event = "message";
                            }
                        }
                    }
                    return null;
                });
        AiSpeechContracts.AgentResponse result = completed.get();
        if (result == null) throw new IllegalStateException("ai-speech stream ended without result");
        return result;
    }

    public ConversationInsightResult insight(ConversationInsightInput input) {
        requireEnabled();
        var messages = input.messages().stream()
                .map(message -> new AiSpeechContracts.Message(message.speaker(), message.content()))
                .toList();
        AiSpeechContracts.InsightResponse response = request("/internal/ai/insights")
                .body(new AiSpeechContracts.InsightRequest(messages))
                .retrieve().body(AiSpeechContracts.InsightResponse.class);
        if (response == null) throw new IllegalStateException("ai-speech returned no insight");
        return new ConversationInsightResult(
                response.summary(), response.highlights(), response.actionItems(),
                response.keywords(), response.tone(), response.generatedBy());
    }

    public AiProviderResult expression(AiProviderInput input) {
        requireEnabled();
        List<AiSpeechContracts.Message> context = input.context().stream()
                .map(message -> new AiSpeechContracts.Message(message.speaker(), message.content()))
                .toList();
        AiSpeechContracts.ExpressionResponse response = request("/internal/ai/expression")
                .body(new AiSpeechContracts.ExpressionRequest(
                        input.action().name(), input.language(), input.sourceText(), context))
                .retrieve().body(AiSpeechContracts.ExpressionResponse.class);
        if (response == null) throw new IllegalStateException("ai-speech returned no expression");
        return new AiProviderResult(response.suggestion(), response.provider(), response.fallback());
    }

    private RestClient.RequestBodySpec request(String path) {
        return client.post().uri(path)
                .header(INTERNAL_TOKEN_HEADER, properties.internalToken())
                .contentType(MediaType.APPLICATION_JSON);
    }

    private void handleEvent(
            String event,
            String data,
            AgentStreamListener listener,
            AtomicReference<AiSpeechContracts.AgentResponse> completed) throws Exception {
        if (data.isBlank()) return;
        switch (event) {
            case "delta" -> {
                JsonNode node = objectMapper.readTree(data);
                String content = node.path("content").asText("");
                if (!content.isEmpty()) listener.onDelta(content);
            }
            case "reset" -> listener.onReset();
            case "done" -> completed.set(
                    objectMapper.readValue(data, AiSpeechContracts.AgentResponse.class));
            case "error" -> throw new IllegalStateException("ai-speech stream reported an error");
            default -> {
                // meta and heartbeat events do not modify the answer.
            }
        }
    }

    private void requireEnabled() {
        if (!properties.enabled()) throw new IllegalStateException("ai-speech service is disabled");
        if (properties.internalToken().isBlank()) {
            throw new IllegalStateException("internal service token is not configured");
        }
    }
}
