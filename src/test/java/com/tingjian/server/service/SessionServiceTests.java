package com.tingjian.server.service;

import com.tingjian.server.dao.SessionDao;
import com.tingjian.server.entity.ConversationEntity;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionServiceTests {
    private final SessionDao sessionDao = mock(SessionDao.class);
    private final SessionService service = new SessionService(sessionDao);

    @Test
    void cannotAppendToEndedConversation() {
        when(sessionDao.lockStatus("id", "local-demo")).thenReturn(Optional.of("ENDED"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.addMessage("id", "OTHER", "hello"));

        assertEquals(409, error.getStatusCode().value());
        verify(sessionDao, never()).addMessage(any());
    }

    @Test
    void cannotReadAnotherOwnersConversation() {
        when(sessionDao.find("missing", "local-demo")).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.messages("missing"));

        assertEquals(404, error.getStatusCode().value());
        verify(sessionDao, never()).messages(any());
    }

    @Test
    void endedConversationReturnsCurrentStateWithoutSecondMutation() {
        ConversationEntity ended = new ConversationEntity(
                "id", "local-demo", "demo", "ENDED", null, null);
        when(sessionDao.end(eq("id"), eq("local-demo"), any())).thenReturn(0);
        when(sessionDao.find("id", "local-demo")).thenReturn(Optional.of(ended));

        assertEquals("ENDED", service.end("id").status());
    }
}
