package com.tingjian.aispeech;

public interface AgentModelProvider {
    AgentModelResult complete(AgentModelInput input);

    default AgentModelResult stream(AgentModelInput input, AgentStreamSink sink) {
        AgentModelResult result = complete(input);
        String text = result.text() == null ? "" : result.text();
        for (int index = 0; index < text.length(); index += 8) {
            sink.delta(text.substring(index, Math.min(text.length(), index + 8)));
        }
        return result;
    }
}
