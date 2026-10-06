package com.tingjian.server.service.speech;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AliyunAsrProtocolTests {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void usesChineseAndEnglishHintsForMixedRecognition() throws Exception {
        String request = AliyunAsrProtocol.runTask(
                mapper, "task-id", "paraformer-realtime-v2", "中英混合", 800);

        assertTrue(request.contains("\"language_hints\":[\"zh\",\"en\"]"));
        assertTrue(request.contains("\"heartbeat\":true"));
        assertTrue(request.contains("\"sample_rate\":16000"));
    }

    @Test
    void mapsSingleLanguageModes() {
        assertEquals(java.util.List.of("zh"), AliyunAsrProtocol.languageHints("中文"));
        assertEquals(java.util.List.of("en"), AliyunAsrProtocol.languageHints("English"));
    }
}
