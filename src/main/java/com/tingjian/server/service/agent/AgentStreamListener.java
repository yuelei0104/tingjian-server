package com.tingjian.server.service.agent;

public interface AgentStreamListener {
    void onDelta(String content);

    default void onReset() {
    }
}
