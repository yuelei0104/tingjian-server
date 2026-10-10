package com.tingjian.server.service.agent;

import com.tingjian.server.dto.AgentChatRequest;
import com.tingjian.server.dto.AiContextMessageRequest;
import com.tingjian.server.dto.HistoryListResponse;
import com.tingjian.server.dto.SessionMessageResponse;
import com.tingjian.server.dto.UsageOverviewResponse;
import com.tingjian.server.dto.UsageResponse;
import com.tingjian.server.service.GlossaryService;
import com.tingjian.server.service.HistoryService;
import com.tingjian.server.service.QuickPhraseService;
import com.tingjian.server.service.SessionService;
import com.tingjian.server.service.UsageService;
import com.tingjian.server.service.usage.UsageReservationGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentServiceTests {
    private final SessionService sessionService = mock(SessionService.class);
    private final GlossaryService glossaryService = mock(GlossaryService.class);
    private final QuickPhraseService quickPhraseService = mock(QuickPhraseService.class);
    private final HistoryService historyService = mock(HistoryService.class);
    private final UsageService usageService = mock(UsageService.class);
    private final UsageReservationGateway usageGateway = mock(UsageReservationGateway.class);
    private final AiSpeechAgentGateway gateway = mock(AiSpeechAgentGateway.class);
    private final UsageReservationGateway.Reservation reservation =
            new UsageReservationGateway.Reservation("reservation-1", true);
    private final AgentService service = new AgentService(
            sessionService, glossaryService, quickPhraseService, historyService,
            usageService, usageGateway, gateway);

    @BeforeEach
    void setUp() {
        when(glossaryService.list("owner")).thenReturn(List.of());
        when(quickPhraseService.list("owner")).thenReturn(List.of());
        when(historyService.search("owner", "", 0, 5)).thenReturn(
                new HistoryListResponse(List.of(), 0, 5, 0, false));
        when(usageService.get("owner")).thenReturn(new UsageResponse(
                "FREE", "免费版", "", false,
                LocalDate.now(), LocalDate.now(),
                new UsageOverviewResponse(0, 0, 0, 0), List.of()));
        when(gateway.enabled()).thenReturn(true);
        when(usageGateway.reserve(any(), any(), anyLong(), any())).thenReturn(reservation);
    }

    @Test
    void usesServerOwnedSessionContextAndCommitsCloudUsage() {
        when(sessionService.messages("owner", "session-1")).thenReturn(List.of(
                new SessionMessageResponse(
                        "message-1", "client-1", 1, "OTHER", "服务端上下文", null)));
        when(gateway.respond(any())).thenReturn(new AiSpeechContracts.AgentResponse(
                "request-1", "模型回复", "QWEN:test", false,
                List.of("lookupGlossary"), 1, Instant.now()));

        var response = service.respond("owner", request("session-1"));

        ArgumentCaptor<AiSpeechContracts.AgentRequest> captor =
                ArgumentCaptor.forClass(AiSpeechContracts.AgentRequest.class);
        verify(gateway).respond(captor.capture());
        assertEquals("owner", captor.getValue().userId());
        assertEquals("服务端上下文", captor.getValue().context().getFirst().content());
        assertEquals("模型回复", response.answer());
        verify(usageGateway).commit(reservation);
    }

    @Test
    void gatewayFailureReturnsLocalAnswerAndReleasesUsage() {
        when(gateway.respond(any())).thenThrow(new IllegalStateException("offline"));

        var response = service.respond("owner", request(null));

        assertTrue(response.fallback());
        assertEquals("LOCAL_AGENT_FALLBACK", response.provider());
        verify(usageGateway).release(reservation);
    }

    private static AgentChatRequest request(String sessionId) {
        return new AgentChatRequest(
                "request-1", sessionId, "请帮我回答？", "中文",
                List.of(new AiContextMessageRequest("OTHER", "客户端上下文")));
    }
}
