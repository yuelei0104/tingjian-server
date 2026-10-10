package com.tingjian.aispeech;

public interface AgentStreamSink {
    void delta(String content);

    default void reset() {
    }
}
