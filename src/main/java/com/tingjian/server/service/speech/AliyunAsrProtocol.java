package com.tingjian.server.service.speech;

import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AliyunAsrProtocol {
    private AliyunAsrProtocol() {
    }

    static String runTask(
            ObjectMapper mapper,
            String taskId,
            String model,
            String language,
            int maxSentenceSilence) throws Exception {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("format", "pcm");
        parameters.put("sample_rate", 16000);
        parameters.put("semantic_punctuation_enabled", false);
        parameters.put("max_sentence_silence", maxSentenceSilence);
        parameters.put("multi_threshold_mode_enabled", true);
        parameters.put("punctuation_prediction_enabled", true);
        parameters.put("inverse_text_normalization_enabled", true);
        parameters.put("heartbeat", true);
        List<String> hints = languageHints(language);
        if (!hints.isEmpty()) parameters.put("language_hints", hints);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("task_group", "audio");
        payload.put("task", "asr");
        payload.put("function", "recognition");
        payload.put("model", model);
        payload.put("parameters", parameters);
        payload.put("input", Map.of());

        return mapper.writeValueAsString(Map.of(
                "header", Map.of(
                        "action", "run-task",
                        "task_id", taskId,
                        "streaming", "duplex"),
                "payload", payload));
    }

    static String finishTask(ObjectMapper mapper, String taskId) throws Exception {
        return mapper.writeValueAsString(Map.of(
                "header", Map.of(
                        "action", "finish-task",
                        "task_id", taskId,
                        "streaming", "duplex"),
                "payload", Map.of("input", Map.of())));
    }

    static List<String> languageHints(String language) {
        return switch (language == null ? "" : language) {
            case "中文" -> List.of("zh");
            case "English" -> List.of("en");
            default -> List.of("zh", "en");
        };
    }

    record CloudEvent(Header header, Payload payload) {
    }

    record Header(String event, String error_code, String error_message) {
    }

    record Payload(Output output) {
    }

    record Output(Sentence sentence) {
    }

    record Sentence(String text, boolean sentence_end, boolean heartbeat) {
    }
}
