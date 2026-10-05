package com.tingjian.server.service.insight;

import java.util.List;

public record ConversationInsightInput(List<ConversationInsightMessage> messages) {
}
