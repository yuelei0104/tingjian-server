package com.tingjian.server.service;

import com.tingjian.server.common.BusinessException;
import com.tingjian.server.dao.SessionDao;
import com.tingjian.server.entity.ConversationEntity;
import com.tingjian.server.entity.ConversationMessageEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.addMessage(
                        "local-demo", "id", "client-1", "OTHER", "hello"));

        assertEquals(409, error.errorCode().status().value());
        verify(sessionDao, never()).addMessage(any());
    }

    @Test
    void cannotReadAnotherOwnersConversation() {
        when(sessionDao.find("missing", "local-demo")).thenReturn(Optional.empty());

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.messages("local-demo", "missing"));

        assertEquals(404, error.errorCode().status().value());
        verify(sessionDao, never()).messages(any());
    }

    @Test
    void endedConversationReturnsCurrentStateWithoutSecondMutation() {
        ConversationEntity ended = new ConversationEntity(
                "id", "local-demo", "demo", "ENDED", null, null);
        when(sessionDao.end(eq("id"), eq("local-demo"), any())).thenReturn(0);
        when(sessionDao.find("id", "local-demo")).thenReturn(Optional.of(ended));

        assertEquals("ENDED", service.end("local-demo", "id").status());
    }

    @Test
    void renameTrimsTitleAndReturnsUpdatedSession() {
        ConversationEntity renamed = new ConversationEntity(
                "id", "owner", "课堂笔记", "ENDED", null, null);
        when(sessionDao.rename("id", "owner", "课堂笔记")).thenReturn(1);
        when(sessionDao.find("id", "owner")).thenReturn(Optional.of(renamed));

        var response = service.rename("owner", "id", "  课堂笔记  ");

        assertEquals("课堂笔记", response.title());
        verify(sessionDao).rename("id", "owner", "课堂笔记");
    }

    @Test
    void renameRejectsAnotherOwnersSession() {
        when(sessionDao.rename("id", "owner", "新标题")).thenReturn(0);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.rename("owner", "id", "新标题"));

        assertEquals(404, error.errorCode().status().value());
    }

    @Test
    void retryWithSameClientMessageIdReturnsExistingMessage() {
        var existing = message("client-1", 7L, "OTHER", "hello");
        when(sessionDao.lockStatus("id", "owner")).thenReturn(Optional.of("ACTIVE"));
        when(sessionDao.findMessageByClientId("id", "client-1"))
                .thenReturn(Optional.of(existing));

        var response = service.addMessage(
                "owner", "id", "client-1", "OTHER", "hello");

        assertEquals("client-1", response.clientMessageId());
        assertEquals(7L, response.sequence());
        verify(sessionDao, never()).addMessage(any());
    }

    @Test
    void reusedClientMessageIdWithDifferentPayloadIsRejected() {
        when(sessionDao.lockStatus("id", "owner")).thenReturn(Optional.of("ACTIVE"));
        when(sessionDao.findMessageByClientId("id", "client-1"))
                .thenReturn(Optional.of(message("client-1", 1L, "OTHER", "hello")));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.addMessage(
                        "owner", "id", "client-1", "SELF", "different"));

        assertEquals(409, error.errorCode().status().value());
    }

    @Test
    void messagePageUsesSequenceCursor() {
        var session = new ConversationEntity(
                "id", "owner", "demo", "ACTIVE", null, null);
        when(sessionDao.find("id", "owner")).thenReturn(Optional.of(session));
        when(sessionDao.messagesAfter("id", 3L, 3)).thenReturn(List.of(
                message("client-4", 4L, "OTHER", "four"),
                message("client-5", 5L, "SELF", "five"),
                message("client-6", 6L, "OTHER", "six")));

        var page = service.messagePage("owner", "id", 3L, 2);

        assertEquals(2, page.items().size());
        assertEquals(5L, page.nextAfterSequence());
        assertTrue(page.hasNext());
    }

    @Test
    void activeSessionReturnsLatestOwnedConversation() {
        var session = new ConversationEntity(
                "active-id", "owner", "课堂会话", "ACTIVE", null, null);
        when(sessionDao.findLatestActive("owner")).thenReturn(Optional.of(session));

        var response = service.active("owner");

        assertTrue(response.available());
        assertEquals("active-id", response.session().id());
    }

    @Test
    void activeSessionExplicitlyReportsWhenNothingCanBeResumed() {
        when(sessionDao.findLatestActive("owner")).thenReturn(Optional.empty());

        var response = service.active("owner");

        assertFalse(response.available());
        assertNull(response.session());
    }

    private static ConversationMessageEntity message(
            String clientId, long sequence, String speaker, String content) {
        return new ConversationMessageEntity(
                "message-" + sequence, "id", clientId, sequence,
                speaker, content, null);
    }
}
