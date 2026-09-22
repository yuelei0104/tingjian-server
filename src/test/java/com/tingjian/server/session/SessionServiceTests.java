package com.tingjian.server.session;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SessionServiceTests {
    private final SessionRepository repository = mock(SessionRepository.class);
    private final SessionService service = new SessionService(repository);

    @Test
    void cannotAppendToEndedConversation() {
        when(repository.lockStatus("id", "local-demo")).thenReturn(Optional.of("ENDED"));
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.addMessage("id", "OTHER", "hello"));
        assertEquals(409, error.getStatusCode().value());
        verify(repository, never()).addMessage(any(), any(), any(), any(), any());
    }

    @Test
    void cannotReadAnotherOwnersConversation() {
        when(repository.find("missing", "local-demo")).thenReturn(Optional.empty());
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.messages("missing"));
        assertEquals(404, error.getStatusCode().value());
        verify(repository, never()).messages(any());
    }

    @Test
    void endedConversationReturnsCurrentStateWithoutSecondMutation() {
        var ended = new SessionRepository.SessionView("id", "demo", "ENDED", null, null);
        when(repository.end(eq("id"), eq("local-demo"), any())).thenReturn(0);
        when(repository.find("id", "local-demo")).thenReturn(Optional.of(ended));
        assertSame(ended, service.end("id"));
    }
}
